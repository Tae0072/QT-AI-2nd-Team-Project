package com.qtai.domain.ai.internal;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiBatchRunLogRepository extends JpaRepository<AiBatchRunLog, Long> {

    List<AiBatchRunLog> findByBatchNameOrderByCreatedAtDesc(AiBatchName batchName, Pageable pageable);

    List<AiBatchRunLog> findByStatusOrderByCreatedAtDesc(AiBatchRunStatus status, Pageable pageable);
}
