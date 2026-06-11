# Report — 2026-06-11 코드리뷰 P3b admin-web QT 관리 (AD-02 풀 CRUD)

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `feature/admin-web-qt-passages-dto` (base `origin/dev`) |
| PR 링크 | (PR 생성 후 작성) |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 3 (P3, AD-02) |
| 관련 F-ID | F-06 |
| 계약서 | `DevA_이지윤/reports/2026-06-10_admin-qt-passages-contract-to-jimin.md` (#454, 김지민 확인 완료) |
| 워크플로우 | [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md) |

## 변경 내용 (렌더 B — 풀 CRUD)

AD-02 QT 관리의 임시 타입 `[key:string]:unknown` + generic 컬럼 렌더를, 백엔드 계약 기준 **실타입 + 풀 CRUD 화면**으로 전환.

- `src/api/qtPassages.ts`:
  - 실타입 `QtPassage`(13필드) + `QtPassageStatus` 5종(`pending_review`/`active`/`hidden`/`deletion_notified`/`removed`)
  - 필터 파라미터 `QtPassageListParams`(status/from/to/q) + 등록·수정 공통 `QtPassageRequest`
  - `createQtPassage`(POST)·`updateQtPassage`(PATCH) 신규 + 기존 list/publish/hide
- `src/pages/QtPassagesPage.tsx`:
  - `usePagedList` 재사용 목록 + 명시 컬럼(날짜·본문·제목·상태 Tag(한글)·게시시각·작업)
  - 필터(상태 Select / 기간·검색 Input) + "준비 중" Alert 철거 → **에러 Alert + 재시도**
  - 행 액션(상태별 수정/게시/숨김, Popconfirm) — 계약서 §7 버튼 정책
  - **등록/수정 Modal 폼** + 검증(필수·`YYYY-MM-DD` 형식·bookId 1~66·`startVerse ≤ endVerse`)
- `formatDateTime` 유틸 재사용. 날짜 입력은 기존 AuditLogsPage와 동일하게 `Input`(YYYY-MM-DD)로 통일(dayjs 회피).

### 자가 점검에서 보정한 antd 함정 2건

- **Modal `forceRender`**: `destroyOnClose` + 닫힌 상태 `setFieldsValue`는 폼 언마운트로 값이 안 들어가 **수정 시 빈 폼**이 뜨는 버그 → `forceRender`로 폼 상시 마운트.
- **validateFields 분리**: 검증 실패가 "저장 실패"로 오표시 + unhandled rejection 나던 것을, 검증 실패 시 조기 return으로 분리(모달 유지·필드 메시지).

## 검증 결과

- `npm run typecheck` → 통과(에러 0).
- `npm run build` → 통과(✓ ~4s). 단일청크 경고 = P5c 대상, 본 범위 아님.
- **실데이터 E2E 미수행** — 로컬 admin-server/관리자 인증 미비(P3a 리포트의 로컬 admin 인증 공백 이슈와 동일). 타입·필드·상태값을 계약서와 1:1 대조해 위험은 낮음.

## CI / 자동 리뷰 결과

- 자가 리뷰: 금지패턴·secret 위반 없음, admin-web 게이트(typecheck+build) 통과.
- 브랜치명 `feature/` 준수. PR 규모: 코드 2 + 문서 3 — B(풀 CRUD)라 코드 라인이 P3a보다 큼(단일 화면이라 분리 부적합).
- (PR 생성 후) GitHub Actions·Claude PR 리뷰 결과 추가 예정.

## 남은 리스크 / 후속

- 실데이터 스모크(등록→게시→숨김 왕복)는 백엔드 풀스택 환경에서 후속.
- PATCH를 전체 필드 전송으로 처리(계약서 §3 등록/수정 동일 요청). 백엔드가 부분 수정만 받으면 무관, 다르면 조정.
- 로드맵 다음: P3c `feature/admin-web-notices-dto`(AD-06) → P5c → P5b → P4.
