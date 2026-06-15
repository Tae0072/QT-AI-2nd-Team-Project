package com.qtai.domain.appversion.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 업데이트 예정 항목 영속성 포트.
 */
public interface PendingAppUpdateRepository extends JpaRepository<PendingAppUpdate, Long> {

    /** 상태별 목록(삭제 제외, 최신순). */
    List<PendingAppUpdate> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(PendingUpdateStatus status);

    /** 전체 목록(삭제 제외, 최신순). */
    List<PendingAppUpdate> findByDeletedAtIsNullOrderByCreatedAtDesc();

    /** 단건(삭제 제외). */
    Optional<PendingAppUpdate> findByIdAndDeletedAtIsNull(Long id);
}
