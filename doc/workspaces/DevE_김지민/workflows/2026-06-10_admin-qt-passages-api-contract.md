# 김지민님 전달용: AD-02 QT 본문 관리 API 응답 계약 확인

> ⚠️ 이 문서는 원래 `2026-06-05_shares-죽은코드-정리-플랜.md` 파일에 잘못 덮여 있던 내용을 올바른 파일로 분리한 것이다(2026-06-10 정리). shares 원본 문서는 복구됨.
> 관련: [admin 카카오 인증 계약](2026-06-10_admin-kakao-auth-contract.md) · [AD010206 체크리스트](../todos/2026-06-09_admin-backend-AD010206-체크리스트.md)

- **작성일:** 2026-06-10
- **전달자:** 이지윤 담당 AD-02 구현 브랜치 기준
- **브랜치:** `feature/admin-qt-passages-api`
- **대상 서버:** admin-server, local/dev 기준 8090
- **Base URL:** `/api/v1/admin/qt-passages`

---

## 1. 확인 요청 요약

김지민님, admin-web에서 AD-02 오늘 QT 관리 화면을 연결하기 전에 아래 요청/응답 형태로 연동 가능한지 확인 부탁드립니다.

특히 합의가 필요한 부분은 다음 3가지입니다.

1. 등록/수정 요청을 `bookId` + `chapter` + `startVerse` + `endVerse`로 받을지 확인
2. 목록 응답의 페이징 필드(`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) 사용 가능 여부 확인
3. 상태값을 `DRAFT`, `PUBLISHED`, `HIDDEN` 그대로 화면에서 사용할지 확인

---

## 2. 엔드포인트

| 기능 | Method | Path | 권한 |
|---|---|---|---|
| 목록 | GET | `/api/v1/admin/qt-passages` | ADMIN + OPERATOR/SUPER_ADMIN |
| 등록 | POST | `/api/v1/admin/qt-passages` | ADMIN + OPERATOR/SUPER_ADMIN |
| 수정 | PATCH | `/api/v1/admin/qt-passages/{id}` | ADMIN + OPERATOR/SUPER_ADMIN |
| 게시 | POST | `/api/v1/admin/qt-passages/{id}/publish` | ADMIN + OPERATOR/SUPER_ADMIN |
| 숨김 | POST | `/api/v1/admin/qt-passages/{id}/hide` | ADMIN + OPERATOR/SUPER_ADMIN |

---

## 3. 등록/수정 요청 JSON

- **클래스명:** `com.qtai.domain.qt.web.AdminQtPassageController.AdminQtPassageRequest`
- **파일:** `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/web/AdminQtPassageController.java`

```json
{
  "qtDate": "2026-06-10",
  "bookId": 19,
  "chapter": 23,
  "startVerse": 1,
  "endVerse": 6,
  "title": "오늘의 QT",
  "mainVerseRef": "시편 23:1-6"
}
```

**요청 필드:**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `qtDate` | string(date) | O | QT 날짜 |
| `bookId` | number | O | 성경 권 ID, 1~66 |
| `chapter` | number | O | 장 |
| `startVerse` | number | O | 시작 절 |
| `endVerse` | number | O | 종료 절 |
| `title` | string | O | QT 제목, 최대 200자 |
| `mainVerseRef` | string | X | 대표 구절 표기, 최대 100자 |

> 현재 구현은 `startVerse <= endVerse`만 허용합니다.

---

## 4. 목록 조회 요청

- **클래스명:** `com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery`
- **파일:** `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/ListAdminQtPassagesQuery.java`

**예시:**
```
GET /api/v1/admin/qt-passages?status=DRAFT&from=2026-06-01&to=2026-06-30&q=&page=0&size=20
```

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | X | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `from` | string(date) | X | 시작 날짜 |
| `to` | string(date) | X | 종료 날짜 |
| `q` | string | X | 제목 또는 대표 구절 검색 |
| `page` | number | X | 기본값 0 |
| `size` | number | X | 기본값 20, 서버에서 1~100으로 보정 |

---

## 5. 목록 응답 JSON

- **클래스명:** `com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse`
- **파일:** `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/AdminQtPassageListResponse.java`

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
        "title": "오늘의 QT",
        "mainVerseRef": "시편 23:1-6",
        "status": "DRAFT",
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

---

## 6. 단건 응답 JSON

- **클래스명:** `com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse`
- **파일:** `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/AdminQtPassageResponse.java`

등록, 수정, 게시, 숨김 응답의 `data`는 아래 형태입니다.

```json
{
  "id": 10,
  "qtDate": "2026-06-10",
  "bookId": 19,
  "chapter": 23,
  "startVerse": 1,
  "endVerse": 6,
  "title": "오늘의 QT",
  "mainVerseRef": "시편 23:1-6",
  "status": "PUBLISHED",
  "publishedAt": "2026-06-10T12:10:00",
  "hiddenAt": null,
  "createdAt": "2026-06-10T12:00:00",
  "updatedAt": "2026-06-10T12:10:00"
}
```

---

## 7. 상태값

- **클래스명:** `com.qtai.domain.qt.internal.QtPassageStatus`
- **파일:** `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageStatus.java`

| 상태 | 의미 | 화면 버튼 예시 |
|---|---|---|
| `DRAFT` | 등록됐지만 아직 게시 전 | 수정, 게시 |
| `PUBLISHED` | 게시됨 | 수정, 숨김 |
| `HIDDEN` | 숨김 처리됨 | 수정, 게시 |

> ✅ **2026-06-10 확정(이지윤 협의)**: 최종 상태값은 **6/9 결정 5종** `active / hidden / pending_review / deletion_notified / removed`. 위 3종은 매핑으로 본다 — **`DRAFT→pending_review`, `PUBLISHED→active`, `HIDDEN→hidden`**. admin-web 상태 Tag·버튼도 **5종 기준**으로 맞춘다. (백엔드 enum도 5종으로 정렬 예정)

---

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

**대표 케이스:**

| 상황 | HTTP | code |
|---|---|---|
| 필수값 누락 또는 잘못된 요청 | 400 | `C0002` |
| 인증 없음 | 401 | `M0002` |
| ADMIN 또는 OPERATOR 권한 없음 | 403 | `M0003` 또는 `AD0003` |
| QT 본문 없음 | 404 | `Q0001` |
| 이미 등록된 QT 날짜 | 409 | `C0003` |

---

## 9. 김지민님 확인 필요 사항

아래 중 하나만 답 주시면 됩니다.

1. 위 요청/응답 그대로 admin-web에 연결 가능
2. `bookId` + `chapter` + `startVerse` + `endVerse` 대신 `startVerseId` + `endVerseId`가 필요
3. 목록 응답 페이징 필드명 변경 필요
4. 상태값 표시명 또는 버튼 정책 변경 필요

> 현재 백엔드 구현 기준으로는 **1번이면 바로 연동 가능**합니다.

---

## 10. FE(김지민) 회신 — 2026-06-10

> 기준: admin-web 현재 코드 `admin-web/src/api/qtPassages.ts` · `pages/QtPassagesPage.tsx`. 현재 **목록·게시(publish)·숨김(hide)만 구현**, `QtPassage` 타입은 generic(응답 키 자동 컬럼), **등록/수정 폼은 미구현**, 페이징은 공통 `Page<T>`(`api/types.ts`) 사용.

**§9 택1 답 → "1번(그대로 연동 가능)". ③ 요청필드·④ 상태값도 2026-06-10 이지윤 협의로 확정.**

| §9 항목 | FE 회신 | 근거 |
|---|---|---|
| **1. 그대로 연결 가능** | ✅ 목록·게시·숨김·페이징·에러는 그대로 가능 | 현 FE가 동일 경로 호출 + generic 컬럼 |
| **2. `startVerseId`/`endVerseId` 필요?** | ✅ **확정(2026-06-10)** — `bookId`+`chapter`+`startVerse`+`endVerse` 기준으로 간다. **04 명세 §4.7.2의 `startVerseId`/`endVerseId`는 이 기준으로 갱신** | 04 §4.7.2 → 갱신 예정 |
| **3. 페이징 필드명 변경 필요?** | ❌ 아니오 — **변경 불필요.** `content/page/size/totalElements/totalPages/first/last`가 FE `Page<T>`·`QtPassagesPage` 페이지네이션과 정확히 일치 | `admin-web/src/api/types.ts` |
| **4. 상태값 표시명/버튼 변경 필요?** | ✅ **확정(2026-06-10)** — 최종은 **6/9 결정 5종** `active/hidden/pending_review/deletion_notified/removed`. 3종은 매핑(`DRAFT→pending_review`·`PUBLISHED→active`·`HIDDEN→hidden`). **admin-web Tag·버튼도 5종 기준** | 6/9 결정 ② |
| 엔드포인트·에러 | ✅ OK — 경로 5종·공통 envelope·코드(C0002/M0002/M0003·AD0003/Q0001/C0003) 그대로 수용 | `client.ts`가 `error.code`/`message` 처리 |

**✅ 확정 결과 (2026-06-10 이지윤 협의):**
1. **요청 필드**: `bookId`+`chapter`+`startVerse`+`endVerse` 기준 확정. ✅ **`04_API_명세서.md §4.7.2`를 본 PR에서 이 기준으로 갱신 완료**(Lead 사후 승인은 PR 본문 트래킹, `CLAUDE.md §2`). 별도 문서 저장소 캐논 04 동기화는 추가 필요.
2. **상태값**: **6/9 결정 5종이 최종** — `active/hidden/pending_review/deletion_notified/removed`. 기존 `DRAFT/PUBLISHED/HIDDEN`은 각각 `pending_review/active/hidden`으로 매핑. **admin-web 상태 Tag·버튼도 5종 기준**으로 맞춘다.

**FE 후속:** `qtPassages.ts`의 generic `QtPassage`를 §6 단건 응답(+5종 status)으로 구체화 + `create/update` 함수 + `QtPassagesPage`에 5종 Tag·버튼.
