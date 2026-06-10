-- V25__add_journal_events_outbox_columns.sql
-- journal_events瑜?吏꾩젙???몃옖??뀡 ?꾩썐諛뺤뒪濡?蹂닿컯?쒕떎(由щ럭 H-13 / ?≪뀡 P1-10).
--
-- 1) previous_qt_passage_id
--    ?명듃??QT 蹂몃Ц??諛붾?蹂寃쎌뿉??"?댁쟾 蹂몃Ц"???④꺼 利앸텇 吏묎퀎(?댁쟾 蹂몃Ц -1, ??蹂몃Ц +1)瑜?
--    媛?ν븯寃??섎뒗 ?좊컲??而щ읆. 湲곗〈 ?됱쓣 留됱? ?딅룄濡?nullable.
-- 2) next_attempt_at
--    ?ъ쿂由ш린(JournalEventReprocessor)??諛깆삤???ъ떆???덉빟 ?쒓컖. NULL?대㈃ 利됱떆 ???二쇰줈 PENDING).
--
-- H2 ?명솚: ADD COLUMN????臾몄옣???щ윭 媛??댁뼱 ?????놁쑝誘濡?媛곴컖 遺꾨━?쒕떎.

ALTER TABLE journal_events
    ADD COLUMN previous_qt_passage_id BIGINT NULL;

ALTER TABLE journal_events
    ADD COLUMN next_attempt_at TIMESTAMP NULL;

ALTER TABLE journal_events
    ADD CONSTRAINT fk_journal_events_prev_qt_passage
        FOREIGN KEY (previous_qt_passage_id) REFERENCES qt_passages(id);

-- ?ъ쿂由ш린 ?대쭅 ?몃뜳?? ?곹깭 + ?ㅼ쓬 ?쒕룄 ?쒓컖?쇰줈 due ?대깽?몃? 鍮좊Ⅴ寃?李얜뒗??
CREATE INDEX idx_journal_events_status_next_attempt
    ON journal_events (status, next_attempt_at);
