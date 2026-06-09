# 2026-06-09 MSA 멀티모듈 기반 구축 워크플로우 (dev-msa)

> 작업 브랜치: `feat/msa-multimodule-skeleton` (dev-msa 기반)
> 기준 계획: `2026-06-09_msa-restart-plan.md`
> 목표: 모놀리식 `qtai-server`를 Strangler 방식으로 Gradle 멀티모듈화 → lib-common → service-bible 추출

## TODO (순서대로 진행)

- [x] **0. 사전 정리** — dev-msa를 origin/dev(#340)로 리셋, 불필요한 로컬 변경 삭제, 작업 브랜치 생성, 계획 문서 커밋(`51bd621`)
- [x] **① 멀티모듈 settings/build 골격 + 빌드 통과** — `62e791b`
  - [x] `settings.gradle.kts`에 `pluginManagement`로 플러그인 버전 공유 + `lib-common`·`service-bible` include
  - [x] root `build.gradle.kts`에서 플러그인 버전 제거(공유), 모놀리식 유지
  - [x] lib-common(java-library)·service-bible(boot app, web only) 스켈레톤 + 스모크 테스트
  - [x] 검증: `:lib-common:build`, `:service-bible:build`, root `compileJava` 모두 통과
- [x] **② lib-common (JWT 필터·공통 응답) + 빌드** — `00fb9f5`
  - [x] 공통 응답/예외(`ApiResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`)를 `com.qtai.common.*` 동일 패키지로 공유
  - [x] 검증 전용 `JwtValidator`(공개키) + `JwtAuthenticationFilter` (`security.jwt.public-key` 있을 때만 활성)
  - [x] `RestClientConfig`(서비스 간 동기 호출용)
  - [x] `spring-tx` 추가(dao 예외), BaseEntity/JPA는 ③로 이연
  - [x] 검증: `:lib-common:build` + `:service-bible:build` 통과
- [~] **③ service-bible 도메인 추출 + 빌드** (3/5 완료 → PR #1, 나머지 PR #2)
  - [x] BaseEntity + JPA + 단일 DB(H2 로컬/MySQL env) 설정 (`c479afb`, `83ac03c`)
  - [x] `bible` 도메인 이전 (파일럿, cross-domain 없음) — `c479afb`
  - [x] `music`·`praise` 이전 (cross-domain 없음; music 시드 150MB 미복제로 비활성) — `83ac03c`
  - [x] SecurityConfig + `scanBasePackages="com.qtai"` + Clock·JPA Auditing
  - [x] 부팅 스모크(@SpringBootTest contextLoads) 통과
  - [ ] **(PR #2)** `qt`·`study` 이전 — qt가 note·ai·member api 계약 + 외부 SU 클라이언트 + 스케줄러/시더를 끌고 옴. study는 50+파일(시뮬레이터·용어집·구절해설). cross-domain은 RestClient 어댑터/Mock로 대체
  - [ ] ArchUnit 경계 테스트(PR #2 범위)
- [ ] **④ 후속(Day 2~3)** — service-user(JWT 발급)·service-note(verseId 쿼리)·service-ai(Kafka)·admin-server + 로컬 k8s

## PR #1 범위 (이번)

기반 + 읽기콘텐츠 3도메인: `51bd621`(계획) · `62e791b`(①) · `00fb9f5`(②) · `539e0fc`(문서) · `c479afb`(③-1 bible) · `83ac03c`(③-2 music/praise) · 본 문서 갱신. → `dev-msa` 대상 PR.

## 진행 메모

- **Strangler 원칙**: root 모놀리식(`qtai-server`)을 그대로 두고 모듈을 점진 추출한다. 추출 완료 도메인은 나중에 모놀리식에서 제거한다. 전환 중 코드 일부 중복은 의도된 것.
- **패키지 동일성**: lib-common이 `com.qtai.common.*`를 그대로 제공 → ③에서 도메인 코드를 옮길 때 `import` 수정이 거의 없다.
- **빌드 환경 주의**: Windows에서 Cursor IDE Java 언어서버가 `build/` 폴더를 잠가 `Failed to clean up stale outputs`/`Unable to delete directory` 오류가 반복됨. 대응: `gradlew --stop` 후 해당 모듈 `build/` 폴더 삭제 → 재빌드. (메모리 [[qtai-build-git-toolchain]])
- **검증 명령**: `./gradlew :<module>:build`. 모놀리식 전체 빌드는 Testcontainers(MySQL)·시간 부담이 커, 모듈 단위로 검증.

## 커밋 로그

```
00fb9f5 feat(msa): lib-common 공통 응답·예외·검증전용 JWT 필터·RestClient 설정 (②)
62e791b build(msa): 멀티모듈 골격 + lib-common·service-bible 스켈레톤 (①)
51bd621 docs(msa): MSA 재시작 분리 계획 추가 (2026-06-09)
7eeb31e ci: 보호 브랜치 ... (= origin/dev, #340 기반)
```

## 다음 액션

③ service-bible 도메인 추출. `bible`(의존 없음)부터 파일럿으로 옮겨 JPA/DB 패턴을 확립한 뒤 music → qt/study/praise 순. qt/study/praise는 `client` 패키지의 타 도메인 호출을 RestClient 어댑터 또는 Mock으로 대체해야 하므로 별도 주의.
