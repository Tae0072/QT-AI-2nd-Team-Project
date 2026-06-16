# 리포트 — 관리자웹 TODO(AD-04/06/07/10) + 시뮬레이터 배선

작성 2026-06-15 · 근거: 관리자웹 조치 TODO 워크플로우(2026-06-15, DevE_김지민) · 검증 기준 origin/dev(#668)

## 1. 검증 결과 (체크표시 무시하고 dev 코드 직접 확인)

| 항목 | 코드 상태 | 비고 |
| --- | --- | --- |
| AD-03 지표↔목록 | ✅ dev 반영 | `AdminAiMonitoringQueryRepository` VALIDATING count |
| AD-05 찬양 404 | ✅ dev 반영 | `AdminPraiseController` GET 200 |
| AD-01 대시보드 CTA | ✅ #647 | 딥링크 `/ai-assets`·`/reports?status=`·`/qt-passages?focusId=` |
| AD-08 모니터링 가독성 | ✅ #647 | 상위 N + "그 외 N종" + Tooltip |
| P3 콘솔 경고 | ✅ | destroyOnClose 0 / destroyOnHidden 18 / Spin tip 0 |
| 해설 생성 트리거 | ✅ #649 | `AdminAiExplanationController` + 감사로그 |
| 시뮬레이터 관리 화면 | ✅ #652 | 조회+숨김 |
| 시뮬레이터 승인→게시 배선 | ☐→✅(이번 작업) | 아래 3절 |
| AD-04 신고 처리/반려 | 코드 ✅ / QA ☐→런북 | 처리·감사·알림 코드 완비, 샘플 검증만 남음 |
| AD-06 공지 발행→알림 | 코드 ✅ / QA ☐→런북 | fanout 코드 완비 |
| AD-07 감사 / AD-10 배치 | 코드 ✅ / QA ☐→런북 | 조회 코드 완비 |

결론: "안 된 것"은 코드 1건(시뮬레이터 배선, 이번에 구현) + QA 검증 4건(코드는 있고 샘플 실행만 필요).

## 2. AD-04/06/07/10 — QA 산출물(재현 가능)

- `scripts/qa-seed/admin-qa-seed.sql`: 신고 2건(POST→처리, COMMENT→반려) + 배치 로그 2건(SUCCEEDED/FAILED) 멱등 시딩.
- `doc/workspaces/Lead_강태오/workflows/2026-06-15_admin-qa-ad04-06-07-10-runbook.md`: 로그인→시딩→처리/발행/감사/배치 검증 PowerShell 런북 + 완료 기준 체크.
- 코드 수정 없음(검증 작업). 사용자 dev 환경에서 실행해 결과표 기입.

## 3. 시뮬레이터 승인→게시 배선 — 구현(admin-server)

- `AiAssetReviewService`에 SIMULATOR+QT_PASSAGE 승인→`publishApprovedSimulatorClip`, 숨김→`hidePublishedSimulatorClip` 배선(해설/용어와 대칭). 단위 테스트 3건 추가.
- SIMULATOR payload 계약 신규 정의(Lead 승인): `{title, componentLibraryVersionId, sceneScript}`.
- 상세·후속(service-ai 미러)·검증: `workflows/2026-06-15_simulator-approve-publish-wiring.md`.

## 4. 변경 파일

| 파일 | 종류 |
| --- | --- |
| qtai-server/admin-server/.../ai/internal/AiAssetReviewService.java | 수정(배선) |
| qtai-server/admin-server/.../ai/internal/AiAssetReviewServiceTest.java | 수정(테스트 3↑) |
| scripts/qa-seed/admin-qa-seed.sql | 신규(QA 시드) |
| doc/.../workflows/2026-06-15_admin-qa-ad04-06-07-10-runbook.md | 신규(런북) |
| doc/.../workflows/2026-06-15_simulator-approve-publish-wiring.md | 신규(워크플로우) |
| doc/.../reports/2026-06-15_admin-todo-ad04-10-and-simulator_report.md | 신규(본 리포트) |

## 5. 검증 / 핸드오프

- 빌드: `cd qtai-server; .\gradlew.bat :admin-server:compileJava :admin-server:compileTestJava :admin-server:test --tests "*AiAssetReviewServiceTest"`
- 샌드박스는 JDK21 빌드 불가 → 위 명령으로 사용자 PC 검증 필요.
- 권장 PR 분리(qtai-pr-guard): ① feat(admin) 시뮬레이터 배선(코드+테스트+워크플로우, F-06/F-12) ② docs/chore QA 런북+시드(AD-04/06/07/10).
- 다른 세션의 회원 관리와 충돌: report 도메인 1곳만 접점이나 파일이 갈려 낮음(AD-04는 QA라 코드 미수정 권장).
