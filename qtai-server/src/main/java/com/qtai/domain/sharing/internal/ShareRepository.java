package com.qtai.domain.sharing.internal;

/**
 * 공유 영속성 포트. Spring Data JPA로 구현.
 *
 * 별도 ShareSnapshotRepository를 둘지, 이 Repository로 통합할지 결정 필요.
 */
public interface ShareRepository {

    // TODO: extends JpaRepository<Share, Long>
    // TODO: Optional<Share> findByShareTokenAndRevokedFalse(String shareToken);
    // TODO: Page<Share> findByOwnerIdOrderBySharedAtDesc(Long ownerId, Pageable pageable);
}
