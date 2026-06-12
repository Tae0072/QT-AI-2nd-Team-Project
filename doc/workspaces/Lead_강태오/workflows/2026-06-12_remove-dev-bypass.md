# 2026-06-12 dev-bypass 잔재 삭제 (정식 JWT 단일 보안 체인)

## 배경
- 관리자 자체 아이디/비밀번호 로그인 도입(PR #539)으로 dev에서도 정식 JWT 인증을 쓰게 되어 `dev-bypass` 토글(X-Dev 헤더 우회)은 불필요. dev-bypass=false로 비활성만 해둔 상태였고, 잔재를 제거한다.

## 변경
- **삭제**: `DevSecurityConfig.java`, `DevUserIdHeaderFilter.java`, `DevUserIdHeaderFilterTest.java`, `DevMemberSeedRunner.java`(`@ConditionalOnProperty(dev-bypass=true)` 게이트 + Flyway dev-seed가 같은 kakao_id를 먼저 생성해 항상 skip → 죽은 코드).
- **SecurityConfig**: `@ConditionalOnProperty(qtai.security.dev-bypass)` 제거 → **모든 프로파일에서 활성인 단일 보안 체인**. import·javadoc 정리.
- **application-dev.yml / prod.yml / demo.yml**: `qtai.security.dev-bypass` 키·관련 주석 제거(prod의 `qtai.retention`은 유지, dev의 `cors.allowed-origins`는 유지).
- **PasswordEncoderConfig**: 주석에서 DevSecurityConfig 언급 제거.

## 안전 확인
- `DevSecurityConfig`/`DevUserIdHeaderFilter`/`DevMemberSeedRunner`를 참조하는 다른 코드 없음(grep 전수). 삭제 후 dangling 참조 0건.
- `SecurityConfig`를 무조건 활성으로 바꿔도, DevSecurityConfig가 사라졌으므로 SecurityFilterChain 빈은 정확히 1개.

## 검증
- `./gradlew :admin-server:test` → **BUILD SUCCESSFUL (1m23s)**. 보안 통합 테스트(`AdminServerSecurityTest`)·`AdminAuthControllerTest`·`AdminAuthServiceTest`·ArchUnit/Modulith 모두 통과(정식 체인 단독으로 컨텍스트 로드 확인).

## Git/PR
- 브랜치 `chore/remove-dev-bypass` → PR 대상 `dev`. 관련: PR #539(아이디/비번 로그인), #546(CLAUDE.md), 문서레포 #20.
