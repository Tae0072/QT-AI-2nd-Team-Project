-- V36__add_admin_username_password.sql
-- 관리자 웹 자체 아이디/비밀번호 로그인(카카오 대체)을 위해 admin_users에 인증 컬럼 추가.
-- username: 로그인 아이디(UNIQUE). password_hash: BCrypt 해시(평문 저장 금지, CLAUDE.md §8/§9).
-- 기존 행 호환을 위해 NULL 허용으로 추가하고, 시드/발급 시 채운다.
-- H2(MODE=MySQL)·MySQL 양립을 위해 컬럼/제약을 분리된 ALTER로 추가한다.

ALTER TABLE admin_users ADD COLUMN username VARCHAR(100);
ALTER TABLE admin_users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE admin_users ADD CONSTRAINT uk_admin_users_username UNIQUE (username);
