# 2026-06-10 dev 관리자 계정 시드 + 관리자 카카오 인증 API 계약

## 목표
service-user 인증 BE 작업의 **선행 토대** 2건:
- **task 4**: dev 전용 관리자 계정 시드(`members.role=ADMIN` + `admin_users.admin_role`).
- **task 5**: 관리자 카카오 인증 API(`POST /api/v1/admin/auth/kakao`) 응답 형태 계약 제안(김지민 합의용).

후속 task 1(VerifyAdminRole RestClient)·task 3(관리자 인증 구현)·task 2(retention 활성화)의 기반.

## 배경 (dev-msa 기준 조사)
- 새 MSA(dev-msa)는 **단일 DB** + admin-server가 Flyway 마이그레이션 소유(모놀리식 통째 복사). 나머지 서비스는 `ddl-auto=validate`.
- `admin_users`는 `V16__create_admin.sql`(member_id UNIQUE FK→members, admin_role VARCHAR(30), status). `members`(`V1`)는 `kakao_id` BIGINT UNIQUE, `role` VARCHAR(10) DEFAULT 'USER'.
- 최신 마이그레이션 V29 → 다음 V30. 단, dev 관리자 계정을 **운영에 적용하면 보안 위험** → 버전 마이그레이션이 아니라 **dev 전용 location + repeatable**로 격리.

## 작업 내용
### task 4 — dev 관리자 시드
1. `admin-server/.../db/dev-seed/R__seed_dev_admin_account.sql`(신규, 멱등 repeatable):
   - members에 관리자 회원 생성(없으면) + 기존 USER면 ADMIN 승격 + `admin_users` 연결(없으면) + 역할/상태 동기화.
   - 모든 INSERT는 `NOT EXISTS` 가드 → 재실행 안전. H2(MODE=MySQL)·MySQL 양립 구문만.
   - 파라미터: `${devAdminKakaoId}`(기본 9000000001)·`${devAdminNickname}`(개발관리자)·`${devAdminRole}`(SUPER_ADMIN). 실제 dev 관리자 카카오 id로 env 덮어쓰면 그 계정이 ADMIN으로 로그인.
2. `application-local.yml`·`application-dev.yml`의 `spring.flyway`에 `locations: classpath:db/migration,classpath:db/dev-seed` + placeholders 추가. **prod·demo는 base의 migration-only 상속 → 시드 운영 미적용**.

### task 5 — 관리자 인증 API 계약
- `contracts/2026-06-10_admin-kakao-auth-api-contract.md`: 엔드포인트·요청(`kakaoAccessToken`)·성공 응답(`ApiResponse<AdminLoginResponse>` — accessToken/refreshToken/admin{memberId,nickname,role,adminRole,status})·오류(403 ADMIN_USER_NOT_FOUND 등)·FE 합의 포인트 5종. 기존 `LoginResponse`와 일관.

## 범위
- 브랜치: `feature/admin-auth-seed-contract` (base: `dev-msa`)
- 변경: admin-server 시드 1 + 프로파일 2(local/dev) + 계약/문서. **service-user·admin-server 코드 로직 무변경**(시드·설정·문서).

## 검증
- **시드 H2 직접 실행 검증**(샌드박스 H2 2.2.224, MODE=MySQL):
  - 시나리오 A(신규): 1회차 → members 1(role=ADMIN)·admin_users 1(SUPER_ADMIN), 2회차 재실행 → 중복 0(멱등 OK).
  - 시나리오 B(기존 USER): 시드가 ADMIN 승격 + admin_users 생성, member 중복 없음.
- dev-only 격리: prod/demo 프로파일 flyway.locations 무변경(migration-only)으로 운영 미적용 확인.
- admin-server 테스트는 create-drop(Flyway 미실행)이라 시드 미경유 → 별도 H2 검증으로 대체(사유 명시).

## 미해결 / 후속
- **task 1**: service-user `VerifyAdminRoleUseCaseMock` → RestClient 어댑터 + **admin-server 시스템 수신 엔드포인트 신설**(현재 AdminController는 GET /me만, 임의 memberId 검증 엔드포인트 없음).
- **task 3**: `POST /api/v1/admin/auth/kakao` 구현(본 계약 + task 1 의존).
- **task 2**: `MemberRetentionPurgeBatch`에 `@ConditionalOnProperty(qtai.retention.purge.enabled)` 게이트 추가 후 task 1 완료 시 true.
- task 5 계약 김지민 합의 → 확정본 반영.

## 담당
- DevD 이승욱 (service-user 인증 BE)
