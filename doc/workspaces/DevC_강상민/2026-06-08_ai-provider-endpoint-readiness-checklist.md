# AI Provider Endpoint Readiness Checklist

## 1. 목적

이 문서는 AI MSA 분리 과정에서 `ai-service`가 호출할 provider service의 내부 system endpoint 구현 준비 상태를 확인하기 위한 checklist다. endpoint 계약은 `qtai-server/apis/ai-service/openapi.yaml`의 `x-ai-outbound-system-endpoints`를 기준으로 하며, 이 문서는 계약을 변경하지 않고 구현자가 확인할 항목으로 풀어 쓴다.

## 2. 공통 규약

| 항목 | 기준 |
| --- | --- |
| Base prefix | `/api/v1/system/**` |
| 인증 | `Authorization: Bearer {service-token}` |
| 필수 권한 | `SYSTEM_BATCH` |
| 응답 envelope | `ApiResponse<T>` |
| envelope 필드 | `success`, `data`, `error`, `timestamp`, `traceId` |
| error 필드 | `error.code`, `error.message`, 선택적 `error.fields` |
| trace | 요청 `traceparent`를 수용하고 응답 `traceId`에 반영 |
| 쓰기 멱등성 | publish, hide, audit 요청은 `Idempotency-Key` 필수 |
| AI 실패 변환 | `ApiResponse.error(code,message)`는 AI 쪽 `AiClientException`으로 변환 |
| 금지 데이터 | Bible 응답과 fixture에 개역개정, ESV, NIV, 성서유니온, 두란노 본문 포함 금지 |

성공 응답 기본 형태:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-06-08T00:00:00+09:00",
  "traceId": "trace-provider-contract"
}
```

실패 응답 기본 형태:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "invalid request",
    "fields": {
      "field": "reason"
    }
  },
  "timestamp": "2026-06-08T00:00:00+09:00",
  "traceId": "trace-provider-contract"
}
```

## 3. Provider별 endpoint 확인표

### 3.1 QT context 조회

| 항목 | 기준 |
| --- | --- |
| Provider | today-qt |
| AI client | `QtContextClient.getQtContext` |
| Method | `GET` |
| Path | `/api/v1/system/qt/passages/{passageId}/context` |
| Query | 없음 |
| Body | 없음 |
| Header | `Authorization`, 선택적 `traceparent` |
| 응답 | `ApiResponse<QtContextResult>` |
| 중요 조건 | AI adapter는 `viewerId`를 전송하지 않음 |
| 제외 필드 | `cacheStatus` |

`QtContextResult` 필드:

| 필드 | 기준 |
| --- | --- |
| `passageId` | QT passage id |
| `bibleBook` | 성경 권 |
| `chapter` | 장 |
| `startVerse` | 시작 절 |
| `endVerse` | 종료 절 |
| `passageReference` | 표시용 참조 |
| `title` | 제목 |
| `summary` | 요약 |
| `passageContext` | AI 생성/검증에 필요한 허용된 메타/context 블록이며 본문 원문 전체가 아님 |

### 3.2 오늘 QT passage 상태 조회

| 항목 | 기준 |
| --- | --- |
| Provider | today-qt |
| AI client | `QtContextClient.getTodayQtPassageStatus` |
| Method | `GET` |
| Path | `/api/v1/system/qt/passages/today/status` |
| Query | `date=YYYY-MM-DD` |
| Body | 없음 |
| Header | `Authorization`, 선택적 `traceparent` |
| 응답 | `ApiResponse<TodayQtPassageStatus>` |

`TodayQtPassageStatus` 필드:

| 필드 | 기준 |
| --- | --- |
| `qtDate` | 조회 기준 날짜 |
| `exists` | passage 존재 여부 |
| `passageId` | passage가 없으면 `null` 가능 |
| `cacheStatus` | `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY` 중 하나 |

필수 fixture 케이스:

| 케이스 | 기대값 |
| --- | --- |
| 새벽 폴백 | `cacheStatus=STALE_FALLBACK` |
| passage 없음 | `cacheStatus=EMPTY`, `exists=false`, `passageId=null` |

### 3.3 Bible verse 조회

| 항목 | 단건 | 목록 | 범위 |
| --- | --- | --- | --- |
| Provider | bible | bible | bible |
| AI client | `BibleVerseClient.getVerse` | `BibleVerseClient.getVersesByIds` | `BibleVerseClient.getVersesInRange` |
| Method | `GET` | `POST` | `GET` |
| Path | `/api/v1/system/bible/verses/{verseId}` | `/api/v1/system/bible/verses:batch` | `/api/v1/system/bible/verses` |
| Query | 없음 | 없음 | `book`, `chapter`, `startVerse`, `endVerse` |
| Body | 없음 | `{ "verseIds": [16, 17] }` | 없음 |
| Header | `Authorization`, 선택적 `traceparent` | `Authorization`, 선택적 `traceparent` | `Authorization`, 선택적 `traceparent` |
| 응답 | `ApiResponse<BibleVerseResult>` | `ApiResponse<BibleVerseBatchResult>` | `ApiResponse<BibleVerseRangeResult>` |

`BibleVerseResult` 필드:

| 필드 | 기준 |
| --- | --- |
| `verseId` | verse id |
| `bibleBook` | 성경 권 |
| `chapter` | 장 |
| `verse` | 절 |
| `reference` | 표시용 참조 |
| `koreanText` | 허용된 한글 성경 데이터만 |
| `englishText` | KJV만 |

`BibleVerseBatchResult`는 `verses` 배열을 포함한다. `BibleVerseRangeResult`는 `bibleBook`, `chapter`, 선택적 `startVerse`, 선택적 `endVerse`, `verses` 배열을 포함한다.

### 3.4 Study publish / hide

| 항목 | publish | hide |
| --- | --- | --- |
| Provider | today-qt study | today-qt study |
| AI client | `StudyPublishClient.publishApprovedVerseExplanation` | `StudyPublishClient.hidePublishedVerseExplanation` |
| Method | `POST` | `POST` |
| Path | `/api/v1/system/study/verse-explanations:publish` | `/api/v1/system/study/verse-explanations:hide` |
| Header | `Authorization`, `Idempotency-Key`, 선택적 `traceparent` | `Authorization`, `Idempotency-Key`, 선택적 `traceparent` |
| 응답 | `ApiResponse<Void>` | `ApiResponse<Void>` |

publish body:

| 필드 | 기준 |
| --- | --- |
| `bibleVerseId` | 승인 해설 대상 verse id |
| `summary` | 승인된 요약 |
| `explanation` | 승인된 해설 |
| `sourceLabel` | 출처 라벨 |
| `aiAssetId` | AI 산출물 id |
| `approvedAt` | 승인 시각 |

hide body:

| 필드 | 기준 |
| --- | --- |
| `aiAssetId` | 숨김 처리할 AI 산출물 id |

### 3.5 Audit log 기록

| 항목 | 기준 |
| --- | --- |
| Provider | admin-service audit |
| AI client | `AuditLogClient.writeAuditLog` |
| Method | `POST` |
| Path | `/api/v1/system/audit/logs` |
| Header | `Authorization`, `Idempotency-Key`, 선택적 `traceparent` |
| 응답 | `ApiResponse<Void>` |

`AuditLogCommand` 필드:

| 필드 | 기준 |
| --- | --- |
| `adminUserId` | 관리자 사용자 id, 없으면 `null` 가능 |
| `actorType` | AI/배치 작업은 `SYSTEM_BATCH` |
| `actorId` | actor id, 없으면 `null` 가능 |
| `actorLabel` | actor 표시명 |
| `actionType` | 감사 action type |
| `targetType` | 감사 대상 type |
| `targetId` | 감사 대상 id |
| `beforeJson` | 변경 전 JSON |
| `afterJson` | 변경 후 JSON |

### 3.6 Admin/Auth 권한 검증

| 항목 | active | verify | verify-any |
| --- | --- | --- | --- |
| Provider | admin-service | admin-service | admin-service |
| AI client | `AdminAuthClient.getActiveAdmin` | `AdminAuthClient.verifyRole` | `AdminAuthClient.verifyAnyRole` |
| Method | `GET` | `GET` | `GET` |
| Path | `/api/v1/system/admin/auth/active` | `/api/v1/system/admin/auth/verify` | `/api/v1/system/admin/auth/verify-any` |
| Query | `memberId` | `memberId`, `role` | `memberId`, `roles=ROLE_A,ROLE_B` |
| Header | `Authorization`, 선택적 `traceparent` | `Authorization`, 선택적 `traceparent` | `Authorization`, 선택적 `traceparent` |
| 응답 | `ApiResponse<AdminAuthResult>` | `ApiResponse<AdminAuthResult>` | `ApiResponse<AdminAuthResult>` |

`AdminAuthResult` 필드:

| 필드 | 기준 |
| --- | --- |
| `adminUserId` | admin user id |
| `memberId` | member id |
| `adminRole` | `OPERATOR`, `REVIEWER`, `CONTENT_CREATOR`, `SUPER_ADMIN` 중 하나 |

## 4. 실패 모델 확인표

| provider 응답/상황 | AI 변환 기준 |
| --- | --- |
| `success=false`, `error.code=UNAUTHORIZED` | `AiClientException(UNAUTHORIZED)` |
| `success=false`, `error.code=FORBIDDEN` | `AiClientException(FORBIDDEN)` |
| `success=false`, `error.code=NOT_FOUND` | `AiClientException(NOT_FOUND)` |
| `success=false`, `error.code=VALIDATION_FAILED` | `AiClientException(VALIDATION_FAILED)` |
| HTTP 401 | `AiClientException(UNAUTHORIZED)` |
| HTTP 403 | `AiClientException(FORBIDDEN)` |
| HTTP 404 | `AiClientException(NOT_FOUND)` |
| HTTP 429 | `AiClientException(RATE_LIMITED)` |
| HTTP 5xx | `AiClientException(DOWNSTREAM_ERROR)` |
| timeout | `AiClientException(TIMEOUT)` |
| envelope 파싱 실패 | `AiClientException(RESPONSE_MAPPING_FAILED)` |
| 알 수 없는 provider error code | `AiClientException(DOWNSTREAM_ERROR)` |

## 5. 후속 fixture 필수 케이스

| 케이스 | 포함 이유 |
| --- | --- |
| Today QT `STALE_FALLBACK` | 00:00~04:00 KST 폴백 회귀 방지 |
| Today QT `EMPTY` | passage 없음 상태를 장애와 구분 |
| provider `error.fields` | envelope v3.1 확장 필드 호환성 확인 |
| F-15 `blockedReason` | AI Q&A 차단 사유 노출 회귀 방지 |
| F-15 `blocked_reason` | 정책 문서와 DB/내부 명명 차이를 fixture에서 확인 |
| malformed envelope | AI adapter의 `RESPONSE_MAPPING_FAILED` 변환 확인 |

## 6. provider 구현 전 최종 확인

| 확인 항목 | 완료 기준 |
| --- | --- |
| endpoint path | 이 문서와 OpenAPI outbound 계약이 일치 |
| 인증 | `Authorization: Bearer {service-token}` 없으면 401/403 |
| 권한 | `SYSTEM_BATCH` 권한 없으면 401/403 |
| 응답 envelope | 모든 성공/실패 응답이 `ApiResponse<T>` 형태 |
| trace | `traceparent` 수용, 응답 `traceId` 포함 |
| 멱등성 | publish/hide/audit에서 `Idempotency-Key` 없으면 실패 |
| 금지 데이터 | Bible 응답과 fixture에 금지 번역본/본문 데이터 없음 |
| null 정책 | nullable로 명시된 필드 외에는 누락하지 않음 |
| Controller 위치 | provider service에만 개설하고 ai-service OpenAPI `paths`에는 추가하지 않음 |
