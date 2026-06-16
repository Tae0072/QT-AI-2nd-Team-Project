-- qt_passages에 자동수집 시각(collected_at) 컬럼 추가.
-- 게시 시각(published_at, QT 날짜 04:00 KST)과 별개로, 시스템 배치가 성서유니온 범위를
-- 실제로 가져온 시각을 기록한다. 스키마는 admin-server가 단독 소유한다(CLAUDE.md).
-- 기존 행은 수집 시각 미상이므로 NULL 허용(백필하지 않는다).

ALTER TABLE qt_passages ADD COLUMN collected_at TIMESTAMP NULL;
