-- [DEV ONLY] 로컬 QT영상 테스트용 샘플 클립 시딩.
-- 운영/시드 데이터가 아니라 로컬에서 "오늘의 QT 영상" 버튼을 켜 보기 위한 임시 데이터다.
-- 사용: docker exec -i qtai-mysql mysql -uqtai -p<DB_PASSWORD> qtai < scripts/dev-seed-qt-video.sql
--
-- 영상은 저작권 부담이 없는 구글 공개 샘플(ForBiggerBlazes.mp4)을 사용한다.
-- 대상 본문(qt_passage_id)과 book_id는 환경에 맞게 수정한다. (기본: id=5, 고전=46)

SET @qt_passage_id := 5;
SET @book_id       := 46;
SET @video_url     := 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4';
SET @now           := NOW(6);

-- 1) 원본 영상(source_videos) — 이미 있으면 재사용
INSERT INTO source_videos
    (bible_book_id, title, storage_provider, video_url, duration_sec, status, active_unique_key, created_at, updated_at)
SELECT @book_id, '[DEV] 샘플 영상', 'EXTERNAL_URL', @video_url, 15.000, 'ACTIVE', NULL, @now, @now
WHERE NOT EXISTS (
    SELECT 1 FROM source_videos WHERE video_url = @video_url
);

SET @source_video_id := (SELECT id FROM source_videos WHERE video_url = @video_url ORDER BY id LIMIT 1);

-- 2) 승인 클립(qt_video_clips) — 본문당 ACTIVE 1건(uk_qt_video_clips_passage_active)
DELETE FROM qt_video_clips
 WHERE qt_passage_id = @qt_passage_id AND active_unique_key = 'ACTIVE';

INSERT INTO qt_video_clips
    (qt_passage_id, title, source_video_id, composition_type, video_url,
     start_time_sec, end_time_sec, status, active_unique_key, approved_at, created_at, updated_at)
VALUES
    (@qt_passage_id, '[DEV] 오늘의 QT 샘플 영상', @source_video_id, 'SINGLE_CUT', @video_url,
     0.000, 10.000, 'APPROVED', 'ACTIVE', @now, @now, @now);

SELECT id, qt_passage_id, status, start_time_sec, end_time_sec, video_url
  FROM qt_video_clips WHERE qt_passage_id = @qt_passage_id;
