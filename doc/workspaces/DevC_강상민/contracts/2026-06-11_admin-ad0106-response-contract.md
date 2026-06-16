# AD-01/AD-06 응답 계약 전달 문서

| 항목 | 내용 |
| --- | --- |
| 작성자 | 강상민 |
| 전달 대상 | 김지민(admin-web) |
| 작성일 | 2026-06-11 |
| 대상 API | AD-01 관리자 대시보드, AD-06 시스템 공지 |
| 기준 구현 | `qtai-server/admin-server`, `qtai-server/apis/api-v1/openapi.yaml` |
| 목적 | admin-web 임시 DTO(`[key: string]: unknown`)를 실제 응답 타입으로 교체 |

## 공통 계약

- Base URL은 admin-server 기준이며 모든 경로는 `/api/v1/admin/**` 아래에 있다.
- 인증은 `Authorization: Bearer <accessToken>`을 사용한다.
- 모든 JSON 성공 응답은 `ApiResponse<T>` envelope를 사용한다. 단, `POST /api/v1/admin/notices/{id}/hide`는 성공 시 `204 No Content`로 body가 없다.
- 성공 envelope 형태:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-06-11T10:00:00+09:00",
  "traceId": "trace-id"
}
```

- 오류 envelope는 기존 공통 오류 응답을 사용한다.
- 날짜/시간은 OpenAPI 기준 `date-time` 문자열이다. Java 구현은 AD-01 감사 로그가 `OffsetDateTime`, AD-06 공지가 `LocalDateTime`이다.

## AD-01 관리자 대시보드

### Endpoint

| Method | Path | 권한 | 성공 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/dashboard` | `ROLE_ADMIN` + `OPERATOR` 또는 `REVIEWER` 또는 `SUPER_ADMIN` | `200 ApiResponse<AdminDashboardResponse>` |

### Response: `AdminDashboardResponse`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `pendingAiValidationCount` | number(int64) | No | AI 검증 대기 산출물 수. `GetAdminAiMonitoringUseCase.validation.waitingAssets` 매핑 |
| `receivedReportCount` | number(int64) | No | `ReportStatus.RECEIVED` 신고 수 |
| `reviewingReportCount` | number(int64) | No | `ReportStatus.REVIEWING` 신고 수 |
| `todayQt` | `AdminDashboardTodayQt` | No | 오늘 QT 표시 상태. 항상 non-null |
| `recentAuditLogs` | `AdminDashboardRecentAuditLog[]` | No | 최근 감사 로그. 없으면 빈 배열 |

### `AdminDashboardTodayQt`

| 필드 | 타입 | Nullable | 값/설명 |
| --- | --- | --- | --- |
| `qtDate` | string(date) | No | KST 기준 날짜, 예: `2026-06-10` |
| `qtPassageId` | number(int64) | Yes | 표시 가능한 QT 본문 ID. 없으면 `null` |
| `title` | string | Yes | QT 제목. 없으면 `null` |
| `status` | string | No | `READY`, `MISSING` |
| `simulatorStatus` | string | Yes | `READY`, `MISSING`, `FAILED`, `DISABLED`, 또는 `null` |
| `hasExplanation` | boolean | No | 해설 진입 가능 여부 |
| `cacheStatus` | string | Yes | `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY`, 또는 `null` |

`todayQt.status=MISSING`일 때도 `todayQt` 객체는 유지하며, `qtPassageId/title/cacheStatus/simulatorStatus`는 상황에 따라 `null`일 수 있다.

### `AdminDashboardRecentAuditLog`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `id` | number(int64) | No | 감사 로그 ID |
| `adminUserId` | number(int64) | Yes | 관리자 사용자 ID |
| `actorType` | string | No | 예: `ADMIN` |
| `actionType` | string | No | 예: `AI_ASSET_APPROVE` |
| `targetType` | string | No | 예: `AI_GENERATED_ASSET` |
| `targetId` | number(int64) | Yes | 대상 ID |
| `createdAt` | string(date-time) | No | 생성 시각 |

대시보드 감사 로그에는 `beforeJson`, `afterJson`, AI payload, prompt/provider 원문, reason 원문이 포함되지 않는다.

### AD-01 예시

```json
{
  "success": true,
  "data": {
    "pendingAiValidationCount": 3,
    "receivedReportCount": 5,
    "reviewingReportCount": 2,
    "todayQt": {
      "qtDate": "2026-06-10",
      "qtPassageId": 35,
      "title": "오늘의 QT",
      "status": "READY",
      "simulatorStatus": "MISSING",
      "hasExplanation": true,
      "cacheStatus": "HIT"
    },
    "recentAuditLogs": [
      {
        "id": 10,
        "adminUserId": 1,
        "actorType": "ADMIN",
        "actionType": "AI_ASSET_APPROVE",
        "targetType": "AI_GENERATED_ASSET",
        "targetId": 500,
        "createdAt": "2026-06-11T10:00:00+09:00"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-06-11T10:00:00+09:00",
  "traceId": "trace-id"
}
```

## AD-06 시스템 공지

### 권한

모든 AD-06 API는 `ROLE_ADMIN`과 admin role `OPERATOR` 또는 `SUPER_ADMIN` 권한이 필요하다. `REVIEWER`, `CONTENT_CREATOR`, 일반 사용자, 미인증 사용자는 거부된다.

### 목록 조회

| Method | Path | Query | 성공 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/notices` | `page` default `0`, `size` default `20`, max `100` | `200 ApiResponse<AdminNoticeListResponse>` |

목록 정렬은 서버에서 `createdAt,desc`로 고정된다.

#### `AdminNoticeListResponse`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `content` | `AdminNoticeListItem[]` | No | 공지 목록 |
| `page` | number(int32) | No | 0-base page |
| `size` | number(int32) | No | 요청 page size |
| `totalElements` | number(int64) | No | 전체 개수 |
| `totalPages` | number(int32) | No | 전체 페이지 수 |
| `first` | boolean | No | 첫 페이지 여부 |
| `last` | boolean | No | 마지막 페이지 여부 |
| `sort` | string | No | `createdAt,desc` |

#### `AdminNoticeListItem`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `id` | number(int64) | No | 공지 ID |
| `title` | string | No | 최대 100자 |
| `bodyPreview` | string | No | 목록용 preview, 최대 83자 |
| `status` | string | No | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `publishedAt` | string(date-time) | Yes | 발행 시각 |
| `createdAt` | string(date-time) | No | 생성 시각 |
| `updatedAt` | string(date-time) | No | 수정 시각 |

### 생성

| Method | Path | 성공 |
| --- | --- | --- |
| POST | `/api/v1/admin/notices` | `201 ApiResponse<AdminNoticeDetailResponse>` |

#### Request: `AdminNoticeRequest`

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `title` | string | Yes | 1~100자, 평문만 허용, `<`, `>` 거부 |
| `body` | string | Yes | 1~10000자, 평문만 허용, `<`, `>` 거부 |
| `status` | string | No | POST는 생략 또는 `DRAFT`만 허용 |

성공 시 `Location: /api/v1/admin/notices/{id}` 헤더가 내려간다.

### 수정

| Method | Path | 성공 |
| --- | --- | --- |
| PATCH | `/api/v1/admin/notices/{id}` | `200 ApiResponse<AdminNoticeDetailResponse>` |

- DRAFT 상태 공지만 수정 가능하다.
- `PUBLISHED` 또는 `HIDDEN` 공지는 `409 INVALID_STATUS_TRANSITION` 계열로 실패한다.
- PATCH request에 `status` 필드가 있으면 거부된다.

### 생성/수정 Response: `AdminNoticeDetailResponse`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `id` | number(int64) | No | 공지 ID |
| `title` | string | No | 제목 |
| `body` | string | No | 본문 전체 |
| `status` | string | No | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `publishedAt` | string(date-time) | Yes | 발행 전이면 `null` |
| `createdAt` | string(date-time) | No | 생성 시각 |
| `updatedAt` | string(date-time) | No | 수정 시각 |

### 발행

| Method | Path | 성공 |
| --- | --- | --- |
| POST | `/api/v1/admin/notices/{id}/publish` | `200 ApiResponse<AdminNoticePublishResponse>` |

- DRAFT 공지만 발행 가능하다.
- 발행 시 활성 회원에게 `notifications.type=NOTICE` 알림을 생성한다.
- 일부 알림 생성 실패는 공지 발행을 롤백하지 않고 `notificationResult.failedCount`로 반환한다.

#### `AdminNoticePublishResponse`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `id` | number(int64) | No | 공지 ID |
| `status` | string | No | 항상 `PUBLISHED` |
| `publishedAt` | string(date-time) | No | 발행 시각 |
| `notificationResult` | `AdminNoticeNotificationResult` | No | 알림 fan-out 결과 |

#### `AdminNoticeNotificationResult`

| 필드 | 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `requestedCount` | number(int64) | No | 알림 생성 대상 회원 수 |
| `createdCount` | number(int64) | No | 실제 생성된 알림 수 |
| `failedCount` | number(int64) | No | 실패한 알림 수 |

### 숨김

| Method | Path | 성공 |
| --- | --- | --- |
| POST | `/api/v1/admin/notices/{id}/hide` | `204 No Content` |

- DRAFT 또는 PUBLISHED 공지를 HIDDEN으로 변경한다.
- 성공 응답 body가 없으므로 admin-web API client는 JSON parse를 시도하지 않아야 한다.

## AD-06 상태 코드 요약

| API | 200 | 201 | 204 | 400 | 401 | 403 | 404 | 409 | 500 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `GET /admin/notices` | O | - | - | O | O | O | - | - | O |
| `POST /admin/notices` | - | O | - | O | O | O | - | O | O |
| `PATCH /admin/notices/{id}` | O | - | - | O | O | O | O | O | O |
| `POST /admin/notices/{id}/publish` | O | - | - | - | O | O | O | O | O |
| `POST /admin/notices/{id}/hide` | - | - | O | - | O | O | O | O | O |

## admin-web 반영 포인트

- AD-01/AD-06 모두 공통 `ApiResponse<T>`에서 `data`를 읽는다.
- AD-06 목록은 공통 `Page<T>` 이름이 아니라 `AdminNoticeListResponse` 자체가 페이지 봉투다.
- `publishedAt`, AD-01 `todayQt.qtPassageId/title/simulatorStatus/cacheStatus`, 감사 로그 `adminUserId/targetId`는 nullable 처리한다.
- 공지 숨김 성공은 `204 No Content`라 envelope가 없다.
- 공지 create/update request의 `status`는 create에서만 선택적으로 `DRAFT`를 보낼 수 있고, update에서는 보내지 않는다.
- 공지 입력은 plain text 전용으로 `<`, `>` 문자를 UI에서 사전 차단하거나 서버 400을 표시한다.

## 전달 후 확인할 항목

- AD-01 화면에서 `todayQt.status=MISSING`과 `todayQt` non-null 케이스가 깨지지 않는지 확인.
- AD-06 목록에서 `bodyPreview`를 사용하고 상세/수정 화면에서만 `body` 전체를 사용하는지 확인.
- 숨김 버튼 처리에서 204 응답을 성공으로 처리하는지 확인.
