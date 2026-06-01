# 리포트 — 누락 Flyway 마이그레이션 hotfix

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `hotfix/missing-flyway-migrations` → PR 대상 `dev`
- 성격: 배포 차단급(기동 실패) 수정

## 1. 문제

dev/local/prod 프로파일은 `spring.jpa.hibernate.ddl-auto: validate` + Flyway를 사용한다. 그런데 최근 머지된 엔티티 5개의 Flyway 마이그레이션이 누락되어, Hibernate가 대응 테이블을 찾지 못해 **앱이 기동되지 않는다**. test 프로파일은 `flyway.enabled=false` + `create-drop`이라 CI가 이 결함을 잡지 못했다.

누락 테이블(실제 `@Entity` 기준):

| 테이블 | 출처 PR | 도메인 |
|--------|---------|--------|
| reports | #140 | report |
| mission_definitions | #141 | mission |
| member_mission_progress | #141 | mission |
| admin_users | #134 | admin |
| admin_action_log | #134 | admin |

## 2. 조치

엔티티 매핑(컬럼명·타입·nullable·UNIQUE·인덱스·FK)에 정확히 맞춰 마이그레이션 추가:

- `V16__create_admin.sql` — admin_users(BaseEntity: created/updated/deleted_at), admin_action_log
- `V17__create_reports.sql` — reports(다형 대상, uk_reports_reporter_target, FK members·admin_users)
- `V18__create_mission.sql` — mission_definitions, member_mission_progress(FK members·mission_definitions)

FK 의존 순서를 고려해 admin(V16) → reports(V17) → mission(V18) 순서로 배치했다.

## 3. 검증 및 한계

- 마이그레이션은 V1~V12와 동일한 표준 MySQL DDL 구문이며, 엔티티 매핑과 1:1 수동 대조했다.
- **자동 검증 시도 → 별개 선재 결함 발견:** Flyway 전체를 H2(dev)에 적용해 validate하는 테스트를 시도했으나, **기존 `V13__add_nickname_snapshot_to_sharing_posts.sql`이 H2에서 구문 오류**로 실패했다. 원인은 `ALTER TABLE ... ADD COLUMN a ..., ADD COLUMN b ...`(MySQL 전용 다중 ADD) 구문을 H2가 거부하기 때문. 즉 **dev 프로파일(H2+Flyway)은 V13부터 이미 기동 불가**였다(본 작업과 무관). V13은 이미 적용된 마이그레이션이라 checksum 때문에 수정할 수 없다.
- 따라서 H2 기반 자동 검증은 불가능. 본 PR은 prod(MySQL) 기준 정합성 + 수동 대조로 검증했다.

## 4. 권장 후속 (별도)

1. **CI 드리프트 가드:** Testcontainers MySQL로 Flyway migrate + `ddl-auto=validate` 컨텍스트 로드 테스트를 CI에 추가해 "엔티티-마이그레이션 불일치"를 자동 검출(이번 결함의 근본 방지).
2. **dev 프로파일 환경:** dev가 H2+Flyway를 쓰는 한 V13의 MySQL 전용 구문 때문에 기동 불가. dev를 MySQL로 두거나, 로컬 H2는 `flyway.enabled=false`+`create-drop`로 분리하는 방안 검토 필요.
3. **admin 2개 테이블(#134)**은 원 담당자(김지민/admin)에게 공유.
