# 워크플로우: 2026-06-10 코드리뷰 후속 TODO 일괄 처리 (Lead)

- 일자: 2026-06-11
- 담당: 강태오 (Lead)
- 입력: `doc/workspaces/Lead_강태오/2026-06-10_코드리뷰_TODO_강태오.md` (TODO 1~5)
- 관련 리뷰 원본: `doc/2026-06-10_서버_코드리뷰.md`

## 0. 선행 결정 (Lead)

- **결정 A**: 일일 배치(00:02 SU 수집, 00:05 AI 시딩)는 **도메인 서비스(service-bible/service-ai) 소유**. admin-server 복사본 스케줄러는 토글 off 기본값, 수동/관리자 트리거 경로만 유지.
- **결정 B**: 클라이언트 진입점은 **nginx 리버스 프록시(8080)**. 앱/웹 포트 분기안 기각(클라이언트 수정 부담).
- 처리 범위: TODO 1~5 전부, TODO별 개별 PR 즉시 생성.

## 1. 진행 절차

1. 두 레포 pull 확인 → 구현 레포 로컬 dev가 origin/dev와 분기(53/61 커밋) 발견 → **모든 작업 브랜치를 origin/dev에서 직접 분기**하는 방식으로 회피.
2. TODO별 브랜치 생성 → 구현 → 검증 → 커밋(-F UTF-8 파일) → push → PR(본문 --body-file).
3. 중간 삽입 작업: PR 자동리뷰 모델 `claude-opus-4-7` → `claude-fable-5` 교체(Lead 결정). #468에 합류 시도했으나 #468이 첫 커밋만 담고 머지되어 #469로 분리.

## 2. 산출 PR

| TODO | PR | 브랜치 | 상태 |
|---|---|---|---|
| 1. 배치 이중 실행 차단 (P1) | #468 | bugfix/admin-batch-toggle-off | MERGED |
| (삽입) 리뷰 모델 fable-5 | #469 | chore/ci-review-model-fable5 | MERGED |
| 2. nginx 게이트웨이 8080 (P1) | #470 | feature/dev-gateway-nginx | MERGED |
| 3. jacoco 게이트 복구 (P2) | #471 | chore/jacoco-gate-restore | MERGED |
| 4. 동기화 규칙 문서화 (P2) | #472 | docs/admin-server-sync-rules | MERGED |
| 5. 보안 잔재 정리 (P3) | #473 | bugfix/admin-security-config-cleanup | OPEN(체크 통과 중) |

## 3. 구현 요점

- **TODO 1**: admin-server yml `qt.today-source.sum.enabled` true→false, `ai.daily-qt-verse-seed.enabled` 키 신설(false — 기존엔 키 부재로 @Value 기본 true가 이중 실행 원인). 토글 off 단위테스트 2건.
- **TODO 2**: `deploy/nginx/dev.conf`(라우팅 SSoT) + compose `gateway` + `k8s/40-gateway.yaml`(NodePort 30080). 경로 매핑은 5개 서비스 컨트롤러 @RequestMapping 전수 대조로 확정. 리뷰 초안 대비 보강: `/me/praise-songs`(bible)·`/me/sharing-posts`·`/me/meditation-calendar`(note) 등 /me 하위 예외 3건, SYSTEM_BATCH 내부 엔드포인트(purge 4종·verse-explanations 게시) 403 이중 방어. `/healthz` 추가.
- **TODO 3**: 루트 build.gradle.kts `subprojects` 공통 jacoco. 실측(2026-06-11) LINE 커버리지: lib-common 63.5 / user 58.7 / bible 55.4 / note 58.6 / **ai 19.3 / admin 17.6**(%) → floor는 실측-5%p. CI continue-on-error 제거 + verification 게이트.
- **TODO 4**: `doc/admin-server-sync-rules.md` 3줄 규칙(① 도메인 로직=도메인 서비스 원본 ② admin 고유만 admin-server 직접 수정 ③ Flyway는 admin-server 단독 소유) + CLAUDE.md §1 요약. V30/V32 사례 기록(V30 삭제는 김태혁 P2-1).
- **TODO 5**: SecurityConfig 잔재 permitAll 2줄 제거(/api/v1/auth/kakao·refresh — admin-server에 컨트롤러 부재), h2-console local/dev 프로파일 가드, 부정 경로 테스트 2건 추가.

## 4. 후속/공유 필요

- [ ] TTS 포트: `app_config.dart` ttsBaseUrl(8090)이 admin-server와 충돌 — 이승욱과 8091 등 재지정 합의 (PR #470 본문 명시)
- [ ] service-ai(0.14)·admin-server(0.12) 커버리지 floor 단계적 상향 — 팀 합의
- [ ] 로컬 잔존물 삭제 공지(빈 폴더 5종·빌드 산출물 2종) — PR #473 본문 참고
- [ ] 로컬 dev 브랜치가 origin/dev와 분기(53/61) — 각자 로컬 dev 재설정 권장(`git fetch && git checkout -B dev origin/dev`)
- [ ] 신규 컨트롤러 추가 시 `deploy/nginx/dev.conf`와 `k8s/40-gateway.yaml`(복사본) 경로 등록 필요 — 팀 공지
