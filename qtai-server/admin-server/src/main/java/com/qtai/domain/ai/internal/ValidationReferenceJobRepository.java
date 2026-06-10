package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationReferenceJobRepository extends JpaRepository<ValidationReferenceJob, Long> {

    Optional<ValidationReferenceJob> findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus status);
}
