package com.qtai.domain.sharing.api;

/**
 * 공유 취소 UseCase 포트.
 *
 * 공유 생성자(owner)만 취소 가능. 취소된 공유는 hard delete가 아닌
 * revoked=true 플래그 — 신고/감사 추적 가능하도록 보존.
 */
public interface RevokeShareUseCase {

    // TODO: void revokeShare(Long memberId, Long shareId);
}
