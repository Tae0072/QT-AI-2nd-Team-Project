package com.qtai.domain.sharing.internal;

/**
 * 나눔 게시글 상태.
 *
 * - PUBLISHED : 공개 중
 * - HIDDEN    : 작성자가 비공개 전환 (게시물 숨김, hiddenAt 기록)
 *               원본 노트가 공유 취소되면 자동으로 HIDDEN 전환 (sourceNoteUnsharedAt 기록)
 * - DELETED   : 작성자 또는 관리자에 의해 삭제됨 (소프트 삭제)
 */
public enum SharingPostStatus {
    PUBLISHED,
    HIDDEN,
    DELETED
}
