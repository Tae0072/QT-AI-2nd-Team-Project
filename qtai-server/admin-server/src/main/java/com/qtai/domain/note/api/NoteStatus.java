package com.qtai.domain.note.api;

/**
 * 노트 저장 상태.
 *
 * - DRAFT : 임시저장 (임시저장 버튼 클릭 시)
 * - SAVED : 최종저장 (저장 버튼 클릭 시)
 *
 * 자동 저장 없음 — CLAUDE.md §6 및 역할 분담 회의록 합의 사항.
 */
public enum NoteStatus {
    DRAFT,
    SAVED,
    DELETED
}
