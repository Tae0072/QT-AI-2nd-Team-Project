# 작업 리포트 — 2026-05-28 04 API 명세 에러코드 반영

> **브랜치**: `bugfix/note-category-error-codes`
> **담당**: 김지민
> **협의**: 강태오(Lead) — "04 API 명세 직접 반영하고, 어느 부분에 추가했는지 리포트로 남기면 담당자가 이후 체크" 결정
> **대상 파일(정본)**: `doc/프로젝트 문서/04_API_명세서.md` (git 추적본)

---

## 1. 배경

PR `bugfix/note-category-error-codes`에서 노트 검증 실패를 `INVALID_INPUT`(C0002) 하나로 응답하던 것을 카테고리별 에러코드 4개로 분리했다. 그러나 클라이언트가 실제로 받는 신규 에러코드가 04 API 계약에 반영되지 않아 SSoT 정합성 위반(봇 리뷰 BLOCK)이 지적되었다.

구현부 응답 경로: [GlobalExceptionHandler.java](../../../qtai-server/src/main/java/com/qtai/common/exception/GlobalExceptionHandler.java)의 `code.getCode()`가 `error.code`로 클라이언트에 전달된다.

| 에러코드 (enum 명) | 구현 코드 | HTTP | 발생 조건 |
|---|---|---|---|
| `NOTE_QT_PASSAGE_REQUIRED` | N0004 | 400 | MEDITATION에 qtPassageId 누락 |
| `NOTE_CONTENT_REQUIRED` | N0005 | 400 | 제목·본문 모두 누락 |
| `NOTE_QT_PASSAGE_FORBIDDEN` | N0006 | 400 | 자유 노트에 qtPassageId 전달 |
| `NOTE_VERSE_REQUIRED` | N0007 | 400 | SERMON에 verseIds 누락 |

> N0003은 결번(`NOTE_BODY_REQUIRED` 정의 후 미사용으로 삭제, N0005가 본문 누락 커버).

---

## 2. 04 변경 위치 (담당자 체크용)

`doc/프로젝트 문서/04_API_명세서.md` 3곳을 수정했다.

### (1) §6.2 공통 에러 코드 표 — 4행 추가

`INVALID_INPUT`(400) 아래에 노트 코드 4개를 추가했다.

```
| `NOTE_QT_PASSAGE_REQUIRED` | 400 | 묵상 노트에 QT 본문 ID 누락 (구현 코드 N0004) |
| `NOTE_CONTENT_REQUIRED` | 400 | 노트 제목·본문 모두 누락 (구현 코드 N0005) |
| `NOTE_QT_PASSAGE_FORBIDDEN` | 400 | 자유 노트에 QT 본문 ID 지정 (구현 코드 N0006) |
| `NOTE_VERSE_REQUIRED` | 400 | 설교 노트에 성경 구절 누락 (구현 코드 N0007) |
```

### (2) §4.3.4 노트 생성 — 성공/실패 코드 라인 신규 추가

기존에 성공/실패 코드 라인이 없어 신규로 추가했다.

```
- **성공 코드:** `201 Created`
- **실패 코드:** `400 VALIDATION_ERROR`, `400 NOTE_QT_PASSAGE_REQUIRED`, `400 NOTE_QT_PASSAGE_FORBIDDEN`, `400 NOTE_VERSE_REQUIRED`, `400 NOTE_CONTENT_REQUIRED`, `401 UNAUTHORIZED`, `404 NOT_FOUND`, `409 DUPLICATE_NOTE`
```

### (3) §4.3.6 노트 수정 — 실패 코드 라인에 4개 추가

기존 실패 코드 라인에 노트 코드 4개를 추가했다(기존 항목은 유지).

```
- **실패 코드:** `400 VALIDATION_ERROR`, `400 NOTE_QT_PASSAGE_REQUIRED`, `400 NOTE_QT_PASSAGE_FORBIDDEN`, `400 NOTE_VERSE_REQUIRED`, `400 NOTE_CONTENT_REQUIRED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 DUPLICATE_NOTE`, `409 INVALID_STATUS_TRANSITION`, `422 INVALID_INPUT`
```

---

## 3. 표기 규칙 / 작업 중 발견한 기존 불일치 (담당자 판단 필요)

04 §6.2는 에러코드를 **enum 명**(`INVALID_INPUT`, `DUPLICATE_NOTE` 등)으로 표기하는 기존 컨벤션을 따른다. 신규 4개도 동일하게 enum 명으로 표기하고, 클라이언트가 실제 받는 구현 코드(N0004~N0007)는 설명 칸에 병기했다.

작업 중 아래 **기존 불일치 2건**을 발견했다. 이번 PR 범위(노트 에러코드 분리)와 무관한 선행 이슈라 손대지 않고 보고만 한다.

| # | 불일치 | 상세 |
|---|---|---|
| A | **enum 명 vs 구현 코드 표기 차이** | 04는 `DUPLICATE_NOTE`로 표기하지만 클라이언트는 실제로 `N0002`(`code.getCode()`)를 받는다. 즉 04의 "코드" 칸 값과 응답 `error.code` 값이 일치하지 않는다. 신규 코드도 동일 구조라 설명 칸에 N-코드를 병기했다. 전면 정합은 별도 결정 필요(04를 N-코드로 바꿀지, 응답을 enum 명으로 바꿀지). |
| B | **`INVALID_INPUT` HTTP 코드 표기 차이** | 04 §4.3.6 실패 코드 라인은 `422 INVALID_INPUT`으로 적혀 있으나, 구현(`ErrorCode.INVALID_INPUT`)은 `400 BAD_REQUEST`다. 이번엔 기존 표기를 유지했다(노트 분리와 무관). |

---

## 4. 검증

- `./gradlew -p qtai-server test` BUILD SUCCESSFUL (코드 변경 없음, 문서만 수정)
- 04 수정은 문서이므로 빌드 영향 없음

---

## 5. 후속 / 담당자 액션

- [ ] 강태오: 04 정본 doc repo(`Tae0072/2nd-Team-Project`)에도 동일 반영 여부 확인 (현재는 구현 저장소 `doc/프로젝트 문서/04_API_명세서.md`만 수정)
- [ ] 강태오: §3-A(enum 명 vs N-코드), §3-B(INVALID_INPUT HTTP 코드) 정합 방향 결정
