# 2026-06-10 MSA Day 2 — 사용자/노트/AI 서비스 추출 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-day2` (브랜치 baseline `feature/msa-user-service`, dev-msa=2bac476 기준)
> PR#1(멀티모듈 골격 + lib-common + service-bible: bible·music·praise)은 dev-msa 머지 완료.
> 기준 계획: `2026-06-09_msa-restart-plan.md` §4 Day 2.
> ※ 이 폴더는 PR#2(qt·study) 세션의 `D:\workspace\QT-AI-2nd-Team-Project`와 분리된 worktree다. 빌드 충돌 없음(단 `gradlew --stop`은 데몬 전역 종료라 동시 빌드 시 주의).

## 대상 규모 (사전 점검)

| 서비스 | 도메인(파일수) | 주요 cross-domain | 특이사항 |
|--------|----------------|-------------------|----------|
| **service-user** (8081) | member(44)·notification(12)·mission(15) | member→mission/praise/notification/sharing/report/note/admin, notification→member(in-svc), mission→note | JWT **발급**(JwtProvider·개인키), Kakao OAuth client, DevMemberSeedRunner |
| **service-note** (8083) | note(45)·sharing(38)·report | note→bible, sharing→member/notification/note(in-svc) | verseId 쿼리파라미터, JournalEvent 핸들러 재처리 |
| **service-ai** (8084) | ai(157) | ai→audit/study/qt/bible/admin | **Kafka** 워커·스케줄러, LLM external |

cross-domain 의존 중 같은 서비스 안에 있는 것은 in-process, 다른 서비스 것은 `client/{도메인}/...UseCaseMock`으로 임시 구현(통합 시 RestClient로 교체).

## TODO (순서대로)

- [ ] **Day2-1 service-user 모듈 스켈레톤 + 빌드** — settings include + boot app(web only) 스켈레톤 + 부팅 스모크
- [ ] **Day2-2 member 이전** — api/internal/web/client. JwtProvider(발급), KakaoOAuthClient, AuthController. 타 도메인은 Mock. JPA/DB(H2 로컬·MySQL env)/보안.
- [ ] **Day2-3 notification·mission 이전 + 테스트 + PR** — notification(→member in-svc)·mission(→note Mock). MockMvc 통합테스트·단위·ArchUnit. → service-user PR(base dev-msa)
- [ ] **Day2-4 service-note 모듈** — note·sharing·report제출. verseId 쿼리, JournalEvent 재처리 로그. 테스트·PR.
- [ ] **Day2-5 service-ai 모듈** — ai+Kafka. 사전생성/검증·F-15 Q&A만, 금지(자유챗봇/SSE/RAG) 부재 테스트. 테스트·PR. (최대 작업)
- [ ] **Day2-6 문서 정리** — 워크플로우·리포트·스터디노트 갱신, F-ID 명시

## 한 방 자동머지 원칙 (PR#1 교훈 반영)

- 브랜치 prefix는 **`feature/`** (feat/ 금지 — CI 실패). commit과 push 분리 호출.
- 첫 푸시부터 APPROVE 품질: **Controller MockMvc 통합테스트**, 권한 검증 헬퍼, 표준 페이징 envelope, 도메인 단위테스트, ArchUnit 경계, 광범위 catch 금지, 로그 민감정보 금지.
- 푸시 후 필수체크(qtai-server Build & Test/Flutter/no-cross-merge) green 확인 → claude-review APPROVE면 자동 squash. 빌드가 리뷰보다 느려 자동머지 스킵되면 `gh run rerun`으로 claude-review 재실행.

## 진행 메모

(작업하며 갱신)
