# [요청] AD-05 찬양 큐레이션 — 수정/숨김 admin 엔드포인트 구현 요청

| 항목 | 내용                                                                                                        |
| ---- | ----------------------------------------------------------------------------------------------------------- |
| 작성 | 김지민 (admin-web FE)                                                                                       |
| 대상 | **(담당 확인 필요)** admin-server praise 백엔드 — 강태오(admin-server 골격 #433) 또는 이승욱(praise 도메인) |
| 일자 | 2026-06-11                                                                                                  |
| 상태 | ⏳ 요청 — 회신(계약 확정) 후 FE 연결                                                                        |
| 근거 | 코드리뷰 TODO P5a, `04_API_명세서.md` §4.7.6, F-09                                                          |

## 1. 배경 / 현재 상태

AD-05 찬양 큐레이션 화면에서 **수정·숨김**을 붙이려는데 백엔드가 아직 없습니다.

- **admin-server `domain.praise`**: `api/`(UseCase·DTO: `CreatePraiseUseCase`, `ListPraiseUseCase`, `PraiseCreateRequest`, `PraiseResponse`)만 있고 **`web/` 컨트롤러의 수정·숨김 핸들러가 없음**.
- **admin-web FE 현재 상태**(`src/api/praiseSongs.ts`):
  - `listPraiseSongs`(GET) ✅ · `createPraiseSong`(POST) ✅ — 동작 중으로 표기
  - `hidePraiseSong`(POST hide) — **함수 정의만, 백엔드 없어 화면 미연결** ❌
  - 수정(PATCH) — FE 함수 없음(엔드포인트 확정되면 추가) ❌

## 2. 요청 엔드포인트 (04 §4.7.6 기준)

| 우선      | Method · Path                               | 동작                               | 성공 코드 |
| --------- | ------------------------------------------- | ---------------------------------- | --------- |
| ⭐ 최우선 | `POST /api/v1/admin/praise-songs/{id}/hide` | 숨김(`status=HIDDEN`)              | 204       |
| 다음      | `PATCH /api/v1/admin/praise-songs/{id}`     | 수정(메타데이터)                   | 200       |
| 확인      | `GET` / `POST /api/v1/admin/praise-songs`   | 목록·등록이 실제 동작하는지 재확인 | 200 / 201 |

- 권한: `ADMIN` + `OPERATOR`/`SUPER_ADMIN` (04 §4.7.6).
- 🚫 정책 유지: 가사·음원 파일·직접 YouTube URL 저장 금지. **곡 메타데이터만** (CLAUDE.md §8 / F-09).

## 3. 김지민이 받아야 할 계약 (이게 와야 타입·화면 작업 가능)

**응답 DTO** (현재 FE `PraiseSong` 타입 — 이대로 가능한지 확인):

```
{ id, title, artist, sourceType('CURATED'|'DEVICE'), licenseNote(nullable),
  status('ACTIVE'|'HIDDEN'), createdAt, updatedAt(nullable) }
```

**수정(PATCH) 요청 바디** (등록과 동일 형태 예상 — 확인 필요):

```
{ title, artist, sourceType, licenseNote?, status }
```

**확인 필요 항목**

1. 위 응답 DTO 필드/상태값(`ACTIVE`/`HIDDEN`) 그대로 사용 가능한가?
2. 숨김은 204(본문 없음)인가, 아니면 갱신된 곡 객체(200)를 주는가? (현재 FE `hidePraiseSong`은 곡 객체를 기대 — 204면 FE를 void로 조정)
3. 수정(PATCH) 요청 바디 필드와 부분 수정 여부, `status` 직접 변경 허용 여부.
4. 상태 전이/에러 코드(예: 이미 숨김 재요청 시 409 등), 없는 곡 404.

## 4. 김지민 적용 계획 (회신 후)

- `praiseSongs.ts`에 `updatePraiseSong`(PATCH) 추가, `hidePraiseSong` 응답 형태 계약에 맞춤.
- `PraiseSongsPage`(AD-05)에 **행 액션(수정/숨김)** 연결 — AD-03·AD-02의 Popconfirm + 상태별 버튼 패턴 재사용.
- 검증: `npm run typecheck` + `npm run build`.

## 5. 회신 요청

아래 중 하나만 주시면 됩니다.

1. **위 §2 엔드포인트 + §3 계약 그대로 구현 가능** → FE 즉시 연결
2. 응답 DTO/상태값/요청 바디 **변경 필요** → 변경안 회신
3. 구현 **일정/우선순위** 안내 (지금 당장 어려우면 hide만 먼저도 OK)

> 회신 오면 `feature/admin-web-praise-actions` 브랜치로 FE 연결하겠습니다. 감사합니다 🙏
