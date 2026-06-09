# 작업 리포트 — MSA Day 2 (사용자/노트/AI 서비스 추출)

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-day2`
- **작업자**: 강태오(Lead, AI 보조)
- **관련**: `2026-06-10_msa-day2-services.md`(워크플로우), `2026-06-09_msa-restart-plan.md`(계획)

## 1. 배경
PR#1으로 멀티모듈 골격 + lib-common + service-bible(bible·music·praise)이 dev-msa에 머지됐다. Day 2는 사용자 4서비스 중 나머지 3개(service-user / service-note / service-ai)를 추출한다. PR#2(qt·study)는 별도 세션에서 병행.

## 2. 수행 내용
(각 단계 완료 시 갱신)

- **Day2-1 service-user 스켈레톤**: ✅ 완료 (`a82724b`). settings include + boot app(web+jpa) + @SpringBootTest contextLoads 통과(14s). 운영 ddl-auto=validate, 테스트 create-drop override.
- **Day2-2 member 이전**: ✅ 완료. member 44파일 `com.qtai.domain.member` 그대로 이전. JwtProvider(발급·개인키)→`com.qtai.security`, KakaoOAuthClient, AuthController(POST /api/v1/auth/kakao, permitAll). 외부 의존(admin·note·praise·report·sharing) api 계약 복사 + `member.client.{도메인}` Mock 6종. SecurityConfig(경로별+@EnableMethodSecurity+CORS)·JpaAuditingConfig(Clock)·application.yml(H2/MySQL·Redis·Kakao·JWT env) 추가. DevMemberSeedRunner는 @Profile(dev)+@ConditionalOnProperty(dev-bypass)로 테스트 비활성.
- **Day2-3 notification·mission + PR**: ✅ 완료. notification(12)→member in-svc, mission(15)→`mission.client.note` Mock. NotificationController 표준 페이징 envelope(`com.qtai.user.web.PageResponse`). 테스트 24개 통과 후 service-user PR(base dev-msa).
- **Day2-4 service-note**: _대기(후속 세션)_
- **Day2-5 service-ai (+Kafka)**: _대기(후속 세션)_

## 3. 검증 결과
- `:service-user:compileJava` 통과(메인 컴파일 무오류).
- `:service-user:build --no-daemon` **EXITCODE=0, 테스트 24개 / 실패 0**.
- 테스트 구성: SecurityIntegrationTest(MockMvc 5건 — 카카오로그인 permitAll·토큰발급, 미인증 401, 정상 200, 표준 envelope, USER→admin 403) / JwtProviderTest(4) / MemberServiceTest(5) / NotificationServiceTest(3) / MissionProgressCalculatorTest(2) / DomainBoundaryTest(ArchUnit 2 — cross-domain은 api로만·web→internal 금지) / 부팅 스모크(1).
- 변경 범위: 전부 `qtai-server/service-user`(modified 2 + 신규 파일). monolith `src/`·lib-common·service-bible·타 worktree 무변경.
- 보안 점검: 새 광범위 catch 없음, 로그에 토큰/비밀번호/개인정보 없음(memberId만), 평문 JWT 키 미커밋(env+테스트 런타임 생성).

## 4. 이슈 및 대응
- **Windows build 폴더 잠금** ("Failed to delete some children"): 해당 모듈 `service-user\build` 삭제 후 재빌드로 해소(`--stop` 미사용 — 동시 빌드 데몬 보호).
- **Mockito UnfinishedStubbingException**(NotificationServiceTest): `@Spy Clock` 호출이 `when(...).thenReturn(...)` 인자(알림 객체 생성) 안에서 일어나 미완료 stubbing으로 오인 → 알림 객체를 stubbing 밖에서 미리 생성해 해소.
- **DomainBoundaryTest**: 단순 `notDependOnEachOther`는 합법적 cross-domain api 의존(member→admin.api 등)도 막으므로, `ignoreDependency(target in ..api..)`로 api 경유 의존만 허용하도록 정교화.

## 5. 다음 단계
service-user 추출 후 service-note → service-ai 순. 각 서비스는 첫 푸시 APPROVE 품질로 별도 PR(base dev-msa).
