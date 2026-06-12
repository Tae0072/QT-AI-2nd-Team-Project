# 결과 리포트 — admin-web AI 평가 세트/항목 화면(AD-11) + 후보 버튼 (2026-06-12)

| 항목 | 내용 |
| --- | --- |
| 담당 | 김지민 (admin-web FE) |
| 브랜치 | `feature/admin-ai-evaluations` (base `dev`) |
| 화면 | **AD-11** AI 평가 세트 |
| 관련 F-ID | F-06(관리자 운영) · F-14/F-15(AI 산출물 품질·평가 세트) · 04 §7.3 |
| workflow | doc/workspaces/DevE_김지민/workflows/2026-06-12_admin-ai-평가셋케이스-화면-워크플로우.md |
| 상태 | 구현·검증 완료, PR 진행 |

## 한 일
- **AD-11 신규 화면**: 서버 `AdminAiEvaluationController`(완전 구현)에 admin-web 화면이 없던 것을 연결.
  - `api/aiEvaluations.ts`(신규): 평가 세트(list/create/activate/retire) + 평가 항목(list/create/approve/reject)
    + `createEvaluationCandidate`. 기존 `aiAssets.ts` 패턴(`unwrap()`+`Page<T>`+`PageParams`) 준수.
  - `pages/AiEvaluationsPage.tsx`(신규): 세트 목록/필터/생성/활성·폐기 + **항목 보기 드로어**(세트별 독립
    `usePagedList`)에서 항목 목록/생성/승인·반려. JSON 필드는 `<pre>` 표시 + 생성 시 `JSON.parse` 검증.
  - `constants/menu.ts`/`App.tsx`: AD-11(`/ai-evaluations`) 메뉴·라우트.
- **평가 항목으로 추가(후보 버튼)**: 팀원 `AiAssetsPage` 상세 드로어에 버튼+모달 추가, `createEvaluationCandidate`
  연결. 모달은 평가 세트 드롭다운만(대상유형 필터) — JSON 없음.
- **쉬운 말 라벨**: "세트/항목"으로 통일(메뉴·버튼·메시지). 사용자 노출 옛 용어 잔여 0.

## 권한
- 페이지 접근: REVIEWER / CONTENT_CREATOR / SUPER_ADMIN(메뉴 requiredRoles + RoleGuard).
- 항목 승인/반려: REVIEWER / SUPER_ADMIN만(`canAccessAdminRoute(role,[REVIEWER])`). CONTENT_CREATOR는 생성까지.
- 후보 버튼: AD-03(REVIEWER 게이트) 안에 있어 접근자 모두 평가관리 권한 보유 → 추가 게이트 불필요.

## 서버 정합성 검증
- 셀렉트 옵션 5종을 서버 enum과 1:1 대조: `AiEvaluationType`(EXPLANATION/SIMULATOR/QA),
  `AiTargetType`(BIBLE_VERSE/QT_PASSAGE/QA_REQUEST), `AiEvaluationSourceType`(VALIDATION_FAILURE/USER_REPORT/ADMIN_CREATED),
  세트 상태(DRAFT/ACTIVE/RETIRED), 항목 상태(CANDIDATE/APPROVED/REJECTED).
- 후보 등록 서버 제약 = `asset.targetType == set.targetType` 하나뿐 → 드롭다운 targetType 필터로 차단.

## 검증
- `npm run typecheck` 클린 · `npm run build` 성공(dev 머지 + 후보버튼/라벨 후 재검증).
- admin-web은 CI에 typecheck/build 잡이 없어 **로컬 검증이 유일 안전망** → 통과 확인.
- 수동: `admin`/`admin1234`(SUPER_ADMIN) 로그인 → AD-11 메뉴 → 세트 생성/활성·폐기 → 항목 생성/승인·반려,
  산출물 상세 → 평가 항목으로 추가.

## 남은 리스크 / 후속
- `AiAssetsPage.tsx`는 팀원(#561) 파일 → 팀원이 추가 수정 시 충돌 가능(현재 다른 작업 중, 합의 하 진행).
- Req2 관리(프롬프트 버전·sourceLabel 수정)는 서버 수정 API 부재로 보류(백엔드 선행). 읽기표시는 팀원 상세에 포함.
- (선택 폴리시) 후보 드롭다운에 RETIRED 세트도 노출됨(서버가 막진 않음). 필요 시 제외 가능.
- **평가 세트 하드 삭제 없음(설계)**: 서버에 `DELETE /evaluation-sets/{id}`가 없고 생명주기는 DRAFT→ACTIVE→RETIRED.
  **폐기(RETIRED)가 소프트 삭제**이며 감사·이력 보존을 위해 목록에 남는다. 목록 조회 `status`가 단일값이라
  "폐기 제외(DRAFT+ACTIVE)"를 한 번에 못 줘 FE-only로 깔끔히 숨기기 어렵다. 진짜 삭제/폐기 제외 조회가 필요하면
  AI 도메인 백엔드에 `DELETE` 또는 `status != RETIRED` 옵션을 요청(별도 작업). 현재는 폐기 상태 태그로 구분.
