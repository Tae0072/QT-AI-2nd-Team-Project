# 김지민님 전달용: AD-02 QT 본문 관리 API 응답 계약 확인

작성일: 2026-06-10
전달자: 이지윤 담당 AD-02 구현 브랜치 기준
브랜치: `feature/admin-qt-passages-api`
대상 서버: `admin-server`, local/dev 기준 8090
Base URL: `/api/v1/admin/qt-passages`

## 1. 확인 요청 요약

김지민님, admin-web에서 AD-02 오늘 QT 관리 화면을 연결하기 전에 아래 요청/응답 형태로 연동 가능한지 확인 부탁드립니다.

특히 합의가 필요한 부분은 다음 3가지입니다.

1. 등록/수정 요청을 `bookId + chapter + startVerse + endVerse`로 받을지 확인
2. 목록 응답의 페이징 필드(`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) 사용 가능 여부 확인
3. 상태값은 6/9 모더레이션 결정에 맞춰 `active`, `hidden`, `pending_review`, `deletion_notified`, `removed` 5종으로 사용

## 2. 엔드포인트

| 기능 | Method | Path | 권한 |
| --- | --- | --- | --- |
| 목록 | GET | `/api/v1/admin/qt-passages` | ADMIN + OPERATOR/SUPER_ADMIN |
| 등록 | POST | `/api/v1/admin/qt-passages` | ADMIN + OPERATOR/SUPER_ADMIN |
| 수정 | PATCH | `/api/v1/admin/qt-passages/{id}` | ADMIN + OPERATOR/SUPER_ADMIN |
| 게시 | POST | `/api/v1/admin/qt-passages/{id}/publish` | ADMIN + OPERATOR/SUPER_ADMIN |
| 숨김 | POST | `/api/v1/admin/qt-passages/{id}/hide` | ADMIN + OPERATOR/SUPER_ADMIN |

## 3. 등록/수정 요청 JSON

작성자: 이지윤 담당 AD-02 구현안, Codex 작성
클래스명: `com.qtai.domain.qt.web.AdminQtPassageController.AdminQtPassageRequest`
파일: `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/web/AdminQtPassageController.java`

```json
{
  "qtDate": "2026-06-10",
  "bookId": 19,
  "chapter": 23,
  "startVerse": 1,
  "endVerse": 6,
  "title": "관리자 QT",
  "mainVerseRef": "시 23:1-6"
}
```

요청 필드 의미:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `qtDate` | string(date) | O | QT 날짜 |
| `bookId` | number | O | 성경 권 ID, 1~66 |
| `chapter` | number | O | 장 |
| `startVerse` | number | O | 시작 절 |
| `endVerse` | number | O | 종료 절 |
| `title` | string | O | QT 제목, 최대 200자 |
| `mainVerseRef` | string | X | 대표 구절 표기, 최대 100자 |

현재 구현은 `startVerse <= endVerse`만 허용합니다.

## 4. 목록 조회 요청

작성자: 이지윤 담당 AD-02 구현안, Codex 작성
클래스명: `com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery`
파일: `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/ListAdminQtPassagesQuery.java`

예시:

```http
GET /api/v1/admin/qt-passages?status=pending_review&from=2026-06-01&to=2026-06-30&q=관리자&page=0&size=20
```

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | string | X | `active`, `hidden`, `pending_review`, `deletion_notified`, `removed` |
| `from` | string(date) | X | 시작 날짜 |
| `to` | string(date) | X | 종료 날짜 |
| `q` | string | X | 제목 또는 대표 구절 검색 |
| `page` | number | X | 기본값 0 |
| `size` | number | X | 기본값 20, 서버에서 1~100으로 보정 |

## 5. 목록 응답 JSON

작성자: 이지윤 담당 AD-02 구현안, Codex 작성
클래스명: `com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse`
파일: `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/AdminQtPassageListResponse.java`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "qtDate": "2026-06-10",
        "bookId": 19,
        "chapter": 23,
        "startVerse": 1,
        "endVerse": 6,
        "title": "관리자 QT",
        "mainVerseRef": "시 23:1-6",
        "status": "pending_review",
        "publishedAt": null,
        "hiddenAt": null,
        "createdAt": "2026-06-10T12:00:00",
        "updatedAt": "2026-06-10T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "error": null,
  "timestamp": "2026-06-10T12:00:00+09:00",
  "traceId": "..."
}
```

## 6. 단건 응답 JSON

작성자: 이지윤 담당 AD-02 구현안, Codex 작성
클래스명: `com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse`
파일: `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/AdminQtPassageResponse.java`

등록, 수정, 게시, 숨김 응답의 `data`는 아래 형태입니다.

```json
{
  "id": 10,
  "qtDate": "2026-06-10",
  "bookId": 19,
  "chapter": 23,
  "startVerse": 1,
  "endVerse": 6,
  "title": "관리자 QT",
  "mainVerseRef": "시 23:1-6",
  "status": "active",
  "publishedAt": "2026-06-10T12:10:00",
  "hiddenAt": null,
  "createdAt": "2026-06-10T12:00:00",
  "updatedAt": "2026-06-10T12:10:00"
}
```

## 7. 상태값

작성자: 이지윤 담당 AD-02 구현안, Codex 작성
클래스명: `com.qtai.domain.qt.internal.QtPassageStatus`
파일: `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageStatus.java`

| 상태 | 의미 | 화면 버튼 예시 |
| --- | --- | --- |
| `pending_review` | 등록됐지만 아직 게시 전 | 수정, 게시 |
| `active` | 게시됨 | 수정, 숨김 |
| `hidden` | 숨김 처리됨 | 수정, 게시 |
| `deletion_notified` | 삭제 안내/처리 대기 | 숨김 유지, 후속 정책에 맞춰 처리 |
| `removed` | 제거 완료 | 목록 확인 |

## 8. 에러 응답 형태

공통 envelope를 사용합니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "C0002",
    "message": "title: must not be blank"
  },
  "timestamp": "2026-06-10T12:00:00+09:00",
  "traceId": "..."
}
```

대표 케이스:

| 상황 | HTTP | code |
| --- | --- | --- |
| 필수값 누락 또는 잘못된 요청 | 400 | `C0002` |
| 인증 없음 | 401 | `M0002` |
| ADMIN 또는 OPERATOR 권한 없음 | 403 | `M0003` 또는 `AD0003` |
| QT 본문 없음 | 404 | `Q0001` |
| 이미 등록된 QT 날짜 | 409 | `C0003` |

## 9. 김지민님 확인 필요 사항

아래 중 하나만 답 주시면 됩니다.

1. 위 요청/응답 그대로 admin-web에 연결 가능
2. 목록 응답 페이징 필드명 변경 필요
3. 상태값 표시명 또는 버튼 정책 변경 필요

김지민님 회신 반영:

- 엔드포인트 5종, 목록/단건 응답, 에러 envelope와 코드 체계는 그대로 연동 가능
- 페이징 필드 `content/page/size/totalElements/totalPages/first/last`는 FE `Page<T>`와 일치
- 등록/수정 요청 필드는 `bookId + chapter + startVerse + endVerse` 형태로 FE 연동 가능
- 상태값은 6/9 모더레이션 결정 5종을 기준으로 맞춘다
