-- =============================================================================
-- 관리자웹 QA 샘플 시딩 (AD-04 신고 / AD-10 배치로그)
-- 근거: 관리자웹 조치 TODO 워크플로우 2026-06-15 (DevE_김지민)
-- 대상 DB: qtai (admin-server 소유 테이블: reports, ai_batch_run_logs)
-- 멱등: 모든 INSERT는 NOT EXISTS 가드 → 재실행해도 중복 생성 없음.
--
-- 실행(로컬 docker, PowerShell):
--   Get-Content scripts/qa-seed/admin-qa-seed.sql | `
--     docker exec -i qtai-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" qtai'
-- =============================================================================

-- 1) AD-04: 신고 샘플 2건 — 처리(RESOLVE)용 POST 1건 + 반려(REJECT)용 COMMENT 1건.
--    reporter = 가장 작은 member id(=dev 관리자 회원), target = 가장 작은 나눔글(없으면 1).
INSERT INTO reports
    (reporter_member_id, target_type, target_id, reason, detail, status, created_at)
SELECT m.id, 'POST',
       COALESCE((SELECT MIN(id) FROM sharing_posts), 1),
       'SPAM', 'QA 샘플 신고(POST): AD-04 처리(RESOLVE) 검증용', 'RECEIVED', NOW()
FROM (SELECT MIN(id) AS id FROM members) m
WHERE m.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM reports r
      WHERE r.reporter_member_id = m.id AND r.target_type = 'POST'
        AND r.target_id = COALESCE((SELECT MIN(id) FROM sharing_posts), 1)
  );

INSERT INTO reports
    (reporter_member_id, target_type, target_id, reason, detail, status, created_at)
SELECT m.id, 'COMMENT', 1,
       'ABUSE', 'QA 샘플 신고(COMMENT): AD-04 반려(REJECT) 검증용', 'RECEIVED', NOW()
FROM (SELECT MIN(id) AS id FROM members) m
WHERE m.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM reports r
      WHERE r.reporter_member_id = m.id AND r.target_type = 'COMMENT' AND r.target_id = 1
  );

-- 2) AD-10: 배치 실행 로그 샘플 2건 (성공 SUCCEEDED + 실패 FAILED).
--    실패행은 error_type/error_message/finished_at 로 "실패시각·오류유형·재시도 판단" 검증.
INSERT INTO ai_batch_run_logs
    (batch_name, status, created_count, failed_count, processed_count,
     error_type, error_message, started_at, finished_at, created_at)
SELECT 'AI_DAILY_QT_VERSE_EXPLANATION_SEED', 'SUCCEEDED', 3, 0, 3,
       NULL, '[QA-SAMPLE] success sample', NOW() - INTERVAL 5 MINUTE, NOW() - INTERVAL 4 MINUTE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_batch_run_logs
    WHERE batch_name = 'AI_DAILY_QT_VERSE_EXPLANATION_SEED' AND status = 'SUCCEEDED'
);

INSERT INTO ai_batch_run_logs
    (batch_name, status, created_count, failed_count, processed_count,
     error_type, error_message, started_at, finished_at, created_at)
SELECT 'AI_DAILY_QT_VERSE_EXPLANATION_SEED', 'FAILED', 0, 2, 2,
       'DeepSeekTimeoutException', '[QA-SAMPLE] QA 샘플: 외부 AI 호출 타임아웃(검증용 가짜 로그)',
       NOW() - INTERVAL 3 MINUTE, NOW() - INTERVAL 2 MINUTE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_batch_run_logs
    WHERE batch_name = 'AI_DAILY_QT_VERSE_EXPLANATION_SEED' AND status = 'FAILED'
);

-- 3) 확인용 조회
SELECT '--- 신고 샘플(RECEIVED) ---' AS info;
SELECT id, reporter_member_id, target_type, target_id, reason, status, created_at
FROM reports WHERE detail LIKE 'QA 샘플%' ORDER BY id DESC LIMIT 5;

SELECT '--- 배치 로그 샘플 ---' AS info;
SELECT id, batch_name, status, created_count, failed_count, error_type, finished_at
FROM ai_batch_run_logs WHERE batch_name = 'AI_DAILY_QT_VERSE_EXPLANATION_SEED' ORDER BY id DESC;
