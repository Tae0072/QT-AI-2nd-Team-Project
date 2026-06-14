-- 프로필 사진을 서버 DB에 저장하기 위한 컬럼 추가(음악 audio_data와 동일한 LONGBLOB 패턴).
-- 스키마는 admin-server가 단독 소유한다(CLAUDE.md). members 테이블에 BLOB+메타를 추가한다.
-- 기존 profile_image_url(VARCHAR)은 유지하며, 사진 업로드 시 스트림 경로 URL을 채운다.
-- MySQL/H2 호환을 위해 컬럼 추가는 한 문장씩 둔다.

ALTER TABLE members ADD COLUMN profile_image_data LONGBLOB NULL;
ALTER TABLE members ADD COLUMN profile_image_content_type VARCHAR(80) NULL;
ALTER TABLE members ADD COLUMN profile_image_updated_at TIMESTAMP NULL;
