# 리포트 — CI 마이그레이션 커버리지 가드

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `chore/ci-migration-coverage-guard` → PR 대상 `dev`
- 배경: 이번 세션 #145(reports/mission/admin 테이블 마이그레이션 누락으로 validate 프로파일 기동 실패)의 근본 재발 방지

## 1. 한 줄 요약

"엔티티는 추가했으나 Flyway 마이그레이션을 빠뜨린" 결함을 PR 단계에서 자동 차단하는 정적 테스트(`MigrationCoverageTest`)를 추가했다. Docker/DB 불필요, 어디서나 동작.

## 2. 문제 / 접근

- dev/prod는 `ddl-auto=validate` + Flyway. 엔티티에 대응 테이블이 마이그레이션에 없으면 기동 실패.
- 그러나 test 프로파일은 `flyway off` + `create-drop`이라 CI가 이 드리프트를 못 잡았다(#145가 머지된 원인).
- Testcontainers MySQL(Flyway migrate+validate)·@SpringBootTest 컨텍스트 로드는 Docker/풀컨텍스트 의존이 커서 후속으로 분리하고, **핵심 재발 케이스(테이블 누락)**를 Docker 없이 잡는 정적 가드를 먼저 도입.

## 3. 구현

`MigrationCoverageTest`:
- `ClassPathScanningCandidateComponentProvider`로 `com.qtai`의 모든 `@Entity` 매핑 테이블명 수집(@Table.name 우선).
- `classpath*:db/migration/*.sql`에서 `CREATE TABLE` 대상 테이블명 수집.
- 엔티티 테이블 ⊆ 마이그레이션 테이블 단언. 누락 시 실패하며 누락 테이블명을 메시지로 노출.
- 거짓 통과 방지: 엔티티/마이그레이션 스캔 결과가 비어있지 않은지도 단언.

## 4. 검증

- `./gradlew test --tests *MigrationCoverageTest --no-daemon` → BUILD SUCCESSFUL (현재 dev의 모든 @Entity 테이블이 마이그레이션에 존재함을 확인 — #145로 갭 해소된 상태).

## 5. 한계 / 후속

- **컬럼/타입 단위 정합성 미검증**: Testcontainers(MySQL)로 Flyway migrate + `ddl-auto=validate` 컨텍스트 로드 테스트를 CI에 추가하면 컬럼·nullable·인덱스까지 검증 가능(후속).
- **누락 빈 / 컨텍스트 와이어링 미검증**(#147 류): `@SpringBootTest` 컨텍스트 로드 가드 별도 필요(Redis/외부 클라이언트 설정 정리 선행).
- dev 프로파일 H2+Flyway는 기존 V13 비호환으로 별개 정리 필요(#145 리포트).
