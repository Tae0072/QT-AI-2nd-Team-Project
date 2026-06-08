package com.qtai.domain.sharing.api;

import java.time.LocalDateTime;

/**
 * 원본 노트 삭제 통지 포트 — note 도메인이 호출한다.
 *
 * <p>명세 §4.3.7: 노트 삭제 시 해당 노트로 발행된 나눔 글에
 * {@code source_note_unshared_at}을 기록해 상세 화면에서 "원본이 삭제된 글"
 * 안내를 판단한다. 나눔 글 자체는 스냅샷이므로 PUBLISHED 상태를 유지한다
 * (2026-06-05 Lead 결정: 유지+안내 표시안).
 *
 * <p>기존에는 이 흐름 전체가 미구현이라 노트를 삭제해도 나눔 글이
 * 영구히 "원본 유효"로 노출됐다.
 */
public interface MarkSourceNoteDeletedUseCase {

    /**
     * noteId로 발행된 나눔 글이 있으면 원본 삭제 시각을 기록한다.
     * 발행 글이 없거나 이미 기록돼 있으면 아무것도 하지 않는다(멱등).
     *
     * @param noteId    삭제된 노트 ID
     * @param deletedAt 노트 삭제 시각
     */
    void markSourceNoteDeleted(Long noteId, LocalDateTime deletedAt);
}
