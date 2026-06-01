# 워크플로우 — 풀 컨텍스트 로드 가드 + 기동 차단 버그 수정

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준 문서: `CLAUDE.md` §10 테스트 기준 / MigrationCoverageTest(#150) 후속
- PR: #167 (머지됨)

## 1. 배경

`MigrationCoverageTest`(#150)는 "엔티티 ↔ Flyway 테이블" 정합성을 정적으로 막지만, 빈 와이어링이 깨진 경우(누락 빈, 생성자 모호성)는 못 잡는다. 슬라이스/단위 테스트도 일부 빈만 띄우므로 못 잡는다. 그동안 `@SpringBootTest`가 한 건도 없어 "실제 앱이 뜨는지"가 검증되지 않았다.

## 2. 작업 범위

- 전체 ApplicationContext를 기동하는 `ApplicationContextLoadTest` 추가.
- 가드 통과를 위해 필요한 최소 수정(기동 차단 버그)만 함께 처리.

## 3. 절차

1. test 프로파일(`application-test.yml`)의 placeholder JWT 키를 유효 테스트 RSA 키로 교체(JwtProvider가 생성자에서 키 파싱 → 풀 컨텍스트 기동에 필요). gitleaks allowlist 경로.
2. `ApplicationContextLoadTest`(`@SpringBootTest @ActiveProfiles("test")`) 작성.
3. 기동 실패 원인 분석 → `AiGenerationJobRunner`의 생성자 2개가 모두 package-private + `@Autowired` 없음 → 주입 생성자 결정 실패. 5-인자 생성자에 `@Autowired` 추가.
4. 컨텍스트 로드 테스트 통과 + 전체 회귀 통과 확인 후 PR.

## 4. 정책 준수

- 외부 의존성 없이 동작: test 프로파일 H2(create-drop, Flyway off) + Redis 지연 연결 + DeepSeek 더미. 기동만으로 외부 네트워크 접속 없음.
- ai 도메인(강상민) 1줄 수정은 PR에서 공유.

## 5. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat test --tests "com.qtai.ApplicationContextLoadTest" --no-daemon
.\gradlew.bat test --no-daemon
```
