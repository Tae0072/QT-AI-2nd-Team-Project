package com.qtai.domain.sharing.internal;

/**
 * 나눔 게시글 상태.
 *
 * - PUBLISHED : 공개 중
 * - DELETED   : 작성자 또는 관리자에 의해 삭제됨 (소프트 삭제)
 */
public enum SharingPostStatus {
    PUBLISHED,
    DELETED
}
