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

- [x] **Day2-1 service-user 모듈 스켈레톤 + 빌드** — settings include + boot app(web+jpa, H2/MySQL) + 부팅 스모크. `:service-user:build` 통과 (`a82724b`)
- [x] **Day2-2 member 이전** — api/internal/web/client 44파일 복사(패키지 `com.qtai.domain.member` 유지). JwtProvider(발급·개인키) `com.qtai.security`로 이전, KakaoOAuthClient, AuthController(POST /api/v1/auth/kakao). 타 서비스 의존(admin·note·praise·report·sharing) api 계약 복사 + `member.client.{도메인}/*UseCaseMock` 6종. SecurityConfig(경로별: auth permitAll·system SYSTEM_BATCH·admin denyAll·그외 authenticated, @EnableMethodSecurity, CORS)+JpaAuditingConfig(Clock) 추가. application.yml(H2/MySQL·Redis·Kakao·JWT env). DevMemberSeedRunner는 @Profile(dev)+@ConditionalOnProperty(dev-bypass)로 테스트 비활성.
- [x] **Day2-3 notification·mission 이전 + 테스트 + PR** — notification(12)→member in-svc, mission(15)→note `mission.client.note` Mock. NotificationController는 표준 페이징 envelope(`com.qtai.user.web.PageResponse`)로 반환. 테스트 24개 전부 통과: SecurityIntegrationTest(MockMvc, 카카오로그인 permitAll·미인증 401·정상 200·admin 403), JwtProviderTest, MemberServiceTest, NotificationServiceTest, MissionProgressCalculatorTest, DomainBoundaryTest(ArchUnit: cross-domain은 api로만·web→internal 금지), 부팅 스모크. `:service-user:build` 통과(EXITCODE=0). → service-user PR(base dev-msa)
- [ ] **Day2-4 service-note 모듈** — note·sharing·report제출. verseId 쿼리, JournalEvent 재처리 로그. 테스트·PR.
- [ ] **Day2-5 service-ai 모듈** — ai+Kafka. 사전생성/검증·F-15 Q&A만, 금지(자유챗봇/SSE/RAG) 부재 테스트. 테스트·PR. (최대 작업)
- [ ] **Day2-6 문서 정리** — 워크플로우·리포트·스터디노트 갱신, F-ID 명시

## 한 방 자동머지 원칙 (PR#1 교훈 반영)

- 브랜치 prefix는 **`feature/`** (feat/ 금지 — CI 실패). commit과 push 분리 호출.
- 첫 푸시부터 APPROVE 품질: **Controller MockMvc 통합테스트**, 권한 검증 헬퍼, 표준 페이징 envelope, 도메인 단위테스트, ArchUnit 경계, 광범위 catch 금지, 로그 민감정보 금지.
- 푸시 후 필수체크(qtai-server Build & Test/Flutter/no-cross-merge) green 확인 → claude-review APPROVE면 자동 squash. 빌드가 리뷰보다 느려 자동머지 스킵되면 `gh run rerun`으로 claude-review 재실행.

## 진행 메모

- **2026-06-10 service-user(Day2-2·2-3) 완료.** member(44)·notification(12)·mission(15) `com.qtai.domain.*` 패키지 그대로 이전(Strangler — 모놀리식 `src/` 원본 유지). 변경은 전부 `qtai-server/service-user` 범위(monolith·lib-common·service-bible·타 worktree 무영향).
  - JWT 분리: **발급**(개인키)은 service-user `com.qtai.security.JwtProvider`만, **검증**(공개키)은 lib-common `JwtAuthenticationFilter`/`JwtValidator`(security.jwt.public-key 설정 시 활성). 모놀리식 `com.qtai.security`의 검증 필터/SecurityConfig는 복사하지 않음(lib-common과 중복 방지).
  - 외부 서비스 의존 7종 → api 계약 타입만 복사 + Mock 임시 구현(통합 시 RestClient 교체, 그때 Mock 삭제): `member.client.admin`(VerifyAdminRole), `member.client.note`(PurgeNote), `member.client.praise`(PurgePraise·ListMemberPraiseSong), `member.client.report`(PurgeReport), `member.client.sharing`(PurgeSharing), `mission.client.note`(GetMeditationCalendar). Mock은 안전 기본값(삭제 미수행 0·관리자 아님·빈 집계).
  - 빌드 워크플로우: 호스트 `gradlew :service-user:build --no-daemon`(데몬 전역종료 `--stop` 회피). Windows build 폴더 잠금 시 해당 모듈 `build` 폴더 삭제 후 재시도(1회 발생). 테스트 JWT 키는 `JwtTestKeysContextCustomizerFactory`(test src+spring.factories) 런타임 생성 주입.
  - 검토 2회: 새로 도입한 광범위 `catch(Exception)` 없음(MyPageController 위젯 격리·MissionProgressCoordinator 회원별 격리·JwtProvider 키초기화는 모놀리식 검증된 기존 코드 그대로). 로그 민감정보 없음(memberId만). 평문 키 미커밋(env+런타임생성).
- **남은 Day2:** service-note(Day2-4)·service-ai(Day2-5)는 후속 세션. RestClient 통합(Mock→실호출 교체)은 Day3.
