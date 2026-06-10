-- V24__seed_ai_prompt_versions.sql
-- AI 생성 job 시딩 기반 데이터 (P1-3).
--
-- ai_generation_jobs.prompt_version_id는 ACTIVE prompt version을 참조한다. 시드가 없으면
-- 00:05 해설 시딩(AiDailyQtVerseExplanationSeedService)이 ACTIVE prompt version을 못 찾아
-- 첫 배포 시 매일 ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND로 실패하던 문제를 보강한다.
--
-- prompt 본문은 external.llm 영역에서 관리하므로 여기서는 버전 메타데이터(타입/버전/해시)만 시드한다.
-- content_hash는 본문 부재 placeholder다(실 prompt 자산 등록 시 갱신).
INSERT INTO ai_prompt_versions (prompt_type, version, content_hash, status, created_at)
VALUES ('EXPLANATION', '2026.06.1', 'seed-explanation-2026.06.1', 'ACTIVE', CURRENT_TIMESTAMP);
