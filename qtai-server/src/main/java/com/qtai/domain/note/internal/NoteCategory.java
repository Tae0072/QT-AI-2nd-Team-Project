package com.qtai.domain.note.internal;

/**
 * 노트 카테고리.
 *
 * - MEDITATION : QT 묵상 노트 (qt_passage_id 필수, active_unique_key='ACTIVE' 적용 → 하루 1건)
 * - SERMON     : 설교 노트 (note_verses 절 연결 필수, qt_passage_id=null, 중복 제한 없음)
 * - PRAYER     : 기도제목 노트 (자유 노트, qt_passage_id=null, 중복 제한 없음)
 * - REPENTANCE : 회개 노트 (자유 노트, qt_passage_id=null, 중복 제한 없음)
 * - GRATITUDE  : 감사 노트 (자유 노트, qt_passage_id=null, 중복 제한 없음)
 */
public enum NoteCategory {
    MEDITATION,
    SERMON,
    PRAYER,
    REPENTANCE,
    GRATITUDE
}
