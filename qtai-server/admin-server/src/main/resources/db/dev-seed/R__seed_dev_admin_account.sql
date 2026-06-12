-- R__seed_dev_admin_account.sql
-- dev 전용 관리자 계정 시드 (멱등 repeatable). admin 인증/권한 흐름 개발·테스트용.
--
-- ⚠️ dev 전용: 이 파일은 db/dev-seed location에 있고, 해당 location은 local·dev 프로파일에서만
--    flyway.locations에 포함된다(application-{local,dev}.yml). prod·demo는 classpath:db/migration만
--    로드하므로 이 시드는 운영에 절대 적용되지 않는다.
--
-- 파라미터(flyway.placeholders, 프로파일에서 env 주입 가능):
--   ${devAdminKakaoId}  : 관리자 카카오 사용자 id(members.kakao_id). 실제 dev 관리자의 카카오 id로 덮어쓰면
--                         그 계정이 카카오 로그인 시 ADMIN으로 인식된다. 기본값은 예약된 dev 더미값.
--   ${devAdminNickname} : 관리자 닉네임(members.nickname, UNIQUE).
--   ${devAdminRole}     : admin_users.admin_role (OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN).
--
-- 멱등: 모든 INSERT는 NOT EXISTS 가드 → 재실행(repeatable 재적용)해도 중복 생성 없음.
-- H2(MODE=MySQL)·MySQL 양립 구문만 사용(ON DUPLICATE KEY 등 비표준 회피).

-- 1) 관리자 회원: 없으면 role=ADMIN으로 생성
INSERT INTO members (kakao_id, nickname, status, role)
SELECT t.kakao_id, t.nickname, t.status, t.role
FROM (
    SELECT ${devAdminKakaoId} AS kakao_id,
           '${devAdminNickname}' AS nickname,
           'ACTIVE' AS status,
           'ADMIN'  AS role
) t
WHERE NOT EXISTS (
    SELECT 1 FROM members m WHERE m.kakao_id = ${devAdminKakaoId}
);

-- 2) 이미 존재하던 회원이면 role을 ADMIN으로 승격(USER로 가입돼 있던 경우 대비)
UPDATE members
SET role = 'ADMIN'
WHERE kakao_id = ${devAdminKakaoId}
  AND role <> 'ADMIN';

-- 3) admin_users 연결: 없으면 생성(member_id UNIQUE)
--    username/password_hash: 자체 아이디 로그인용(dev 전용). 비밀번호 평문은 'admin1234'이고,
--    아래 password_hash는 그 BCrypt($2a$10$) 해시다. dev 전용 throwaway 값으로 운영에 적용되지 않는다.
INSERT INTO admin_users (member_id, admin_role, status, username, password_hash)
SELECT m.id, '${devAdminRole}', 'ACTIVE',
       'admin', '$2a$10$4vY2VPLzS964grfCYoSDC.sEoDgcjABk5n3lxU4Ul2/aHUh0ssnHG'
FROM members m
WHERE m.kakao_id = ${devAdminKakaoId}
  AND NOT EXISTS (
      SELECT 1 FROM admin_users au WHERE au.member_id = m.id
  );

-- 4) 이미 admin_users가 있으면 역할/상태/로그인 자격을 시드 기준으로 동기화(soft-delete 해제 포함)
UPDATE admin_users
SET admin_role    = '${devAdminRole}',
    status        = 'ACTIVE',
    deleted_at    = NULL,
    username      = 'admin',
    password_hash = '$2a$10$4vY2VPLzS964grfCYoSDC.sEoDgcjABk5n3lxU4Ul2/aHUh0ssnHG'
WHERE member_id IN (SELECT id FROM members WHERE kakao_id = ${devAdminKakaoId});
