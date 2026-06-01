# 리포트 — 컨텍스트 로드 가드 + 기동 차단 버그 수정

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `test/context-load-guard` → PR 대상 `dev`
- 관련: MigrationCoverageTest(#150) 후속 · CLAUDE.md §10 테스트 기준

## 1. 한 줄 요약

전체 ApplicationContext를 실제로 기동하는 `ApplicationContextLoadTest`를 추가했고, 이 가드가 **지금까지 한 번도 잡히지 않았던 기동 차단 버그**(`AiGenerationJobRunner` 빈 생성 실패)를 찾아내 함께 고쳤다. 전체 회귀 통과.

## 2. 배경 — 기존 가드의 사각지대

- `MigrationCoverageTest`(#150)는 "엔티티 ↔ Flyway 테이블" 정합성을 **정적으로** 막는다.
- 하지만 "빈 와이어링이 깨졌다"(누락 빈, 생성자 모호성, 설정 바인딩 오류)는 정적 검사로 못 잡는다. 슬라이스/단위 테스트도 일부 빈만 띄우므로 못 잡는다.
- 실제 앱이 뜨는지는 **풀 컨텍스트 기동**으로만 검증된다 — 그동안 `@SpringBootTest`가 한 건도 없었다.

## 3. 가드가 잡은 버그

`AiGenerationJobRunner`(ai 도메인, @Service) 기동 실패:

```
BeanCreationException: ... AiGenerationJobRunner: No default constructor found
```

- 원인: 생성자가 2개(① 스프링 주입용 5-인자, ② 테스트용 6-인자)인데 **둘 다 package-private이고 @Autowired가 없어** 스프링이 주입 생성자를 결정하지 못함 → 기본 생성자를 찾다 실패.
- 비교: 같은 "생성자 2개" 패턴인 컨트롤러 5종(AdminAiAssetController 등)은 **public 생성자가 1개**라 스프링이 자동 선택 → 정상. 즉 이 한 곳만 깨져 있었다.
- 영향: 풀 컨텍스트가 뜨지 않으므로 **실제 서버 기동(bootRun)도 실패**하는 상태였다. 슬라이스 테스트만 돌려와 그동안 발견되지 않았다.
- 수정: 5-인자(주입용) 생성자에 `@Autowired` 추가(1줄). 동작 변경 없음.

## 4. 변경 파일

| 구분 | 파일 | 내용 |
|------|------|------|
| 신규 | `src/test/java/com/qtai/ApplicationContextLoadTest.java` | `@SpringBootTest @ActiveProfiles("test")` 풀 컨텍스트 로드 검증 |
| 수정 | `src/test/resources/application-test.yml` | test 프로파일 JWT 키를 placeholder→유효 테스트 RSA 키로 교체(JwtProvider가 생성자에서 키 파싱하므로 풀 컨텍스트 기동에 필요). gitleaks allowlist 적용 경로 |
| 수정 | `src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | 주입 생성자에 `@Autowired` 추가(기동 차단 버그 수정) |
| 신규 | 본 리포트 |

## 5. 검증

- `./gradlew test --tests com.qtai.ApplicationContextLoadTest` → BUILD SUCCESSFUL (수정 전 FAILED → 수정 후 통과).
- `./gradlew test`(전체 회귀) → BUILD SUCCESSFUL in 53s. 신규 가드 포함 전부 통과.
- 외부 의존성 없이 동작: test 프로파일 H2(create-drop, Flyway off) + Redis 지연 연결 + DeepSeek 더미 설정. 기동만으로 외부 네트워크 접속 없음.

## 6. 범위 / 후속

- 본 PR: 풀 컨텍스트 기동 가능 여부까지. 컬럼 단위 스키마 정합성은 후속 Testcontainers(MySQL) Flyway migrate+validate로 보강(2단계 일정).
- ai 도메인(강상민) 코드 1줄을 수정했으므로 PR에서 명시·공유.
