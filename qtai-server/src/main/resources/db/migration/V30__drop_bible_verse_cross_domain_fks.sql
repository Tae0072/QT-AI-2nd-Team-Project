-- V30__drop_bible_verse_cross_domain_fks.sql
-- MSA Inc4 — bible DB-per-service 분리 준비.
-- bible_verses를 참조하는 크로스 도메인 외래키 4개를 제거한다(DB 간 FK 불가).
-- bible_verse_id 컬럼·인덱스·UNIQUE KEY는 유지 — bible 데이터 소유는 bible-service로 이전되고,
-- 각 도메인은 bible_verse_id를 DB 제약 없는 참조 값으로 보유(무결성은 앱/서비스 경계에서 보장).
-- 근거: bible 추출 진행요약 §4-3(2026-06-09 DB 분리 승인). Inc5에서 모놀리식 bible_verses 제거 전 선행.

ALTER TABLE glossary_terms DROP FOREIGN KEY fk_glossary_terms_verse;
ALTER TABLE qt_passage_verses DROP FOREIGN KEY fk_qpv_verse;
ALTER TABLE note_verses DROP FOREIGN KEY fk_nv_verse;
ALTER TABLE verse_explanations DROP FOREIGN KEY fk_verse_explanations_verse;
