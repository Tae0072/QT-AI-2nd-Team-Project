# 리포트: 2026-06-10 코드리뷰 후속 TODO 일괄 처리 결과

- 일자: 2026-06-11 / 담당: 강태오 (Lead)
- 워크플로우: `doc/workspaces/Lead_강태오/workflows/2026-06-11_code-review-todo-batch.md`

## 요약

코드리뷰(2026-06-10) Lead 배정 TODO 5건을 당일 전부 처리했다. PR 6건(#468~#473) 중 5건 머지, 1건(#473) 체크 통과 진행 중. P1 2건(배치 이중 실행, 8080 진입점 부재)이 해소되어 **flutter-app·admin-web이 dev 토폴로지에 다시 연결 가능**해졌고, 외부(성서유니온) 중복 호출과 AiBatchRunLog 이중 기록이 차단됐다.

## 결과

| # | 항목 | PR | 핵심 효과 |
|---|---|---|---|
| 1 | 배치 이중 실행 차단 | #468 (merged) | admin-server 00:02/00:05 스케줄러 기본 off — 도메인 서비스 단독 소유(결정 A) |
| - | 리뷰 모델 교체 | #469 (merged) | PR 자동리뷰 `claude-fable-5` 전환 |
| 2 | nginx 게이트웨이 | #470 (merged) | 8080 단일 진입점 복구(결정 B), 내부 API 403 이중 방어, k8s 포함 |
| 3 | jacoco 게이트 | #471 (merged) | CLAUDE.md §11 명령 동작 복구, CI 조용한 실패 제거, 모듈별 floor(실측-5%p) |
| 4 | 동기화 규칙 | #472 (merged) | admin-server 복사본 드리프트 방지 3줄 규칙 + CLAUDE.md 반영 |
| 5 | 보안 잔재 정리 | #473 (open) | 잔재 permitAll 제거, h2-console 프로파일 가드, 부정 경로 테스트 |

## 검증 기록

- TODO 1·5: `:admin-server:test` 대상 테스트 전부 통과(토글 off 2건, 보안 부정 경로 2건 + 기존 4건).
- TODO 2: nginx conf 구문 파싱 OK, compose/k8s YAML 파싱 OK, flutter-app 호출 경로 전수 커버 확인. 기동 스모크는 머지 후 dev 환경에서 수행 예정.
- TODO 3: `test jacocoTestReport` BUILD SUCCESSFUL, `jacocoTestCoverageVerification` 통과(exit 0).
- 전 PR CI: Requirements Guard·Gitleaks·Spectral·PR Size 등 통과 확인(#473은 진행 중).

## 리스크/잔여 과제

1. **커버리지 격차**: service-ai 19.3%, admin-server 17.6% — 품질 게이트 문서(전체 70%) 대비 큰 격차. floor 단계 상향 계획 필요.
2. **게이트웨이 라우팅 이중 관리**: dev.conf(SSoT)와 k8s ConfigMap 복사본 — 신규 컨트롤러 추가 시 양쪽 갱신 필요(차후 자동화 후보).
3. **TTS 포트 충돌**(8090): 해소 — dev 기본값 8091로 변경(PR #475). TTS는 Lead(강태오) 본인 소관이라 로컬 TTS 서버만 8091로 맞추면 됨.
4. **로컬 dev 분기**: 팀원 로컬 dev가 origin/dev와 갈라졌을 수 있음 — 재설정 공지 필요.
