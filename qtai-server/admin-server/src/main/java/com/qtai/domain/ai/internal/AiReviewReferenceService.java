package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AiReviewReferenceService {

    private final ValidationReferenceJobRepository validationReferenceJobRepository;

    AiReviewReferenceService(ValidationReferenceJobRepository validationReferenceJobRepository) {
        this.validationReferenceJobRepository = validationReferenceJobRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ReferenceMetadata> latestActiveReference() {
        return validationReferenceJobRepository
                .findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus.ACTIVE)
                .map(ReferenceMetadata::from);
    }

    record ReferenceMetadata(
            Long validationReferenceJobId,
            String sourceName,
            String sourceFileHash,
            String indexStorageUri
    ) {

        private static ReferenceMetadata from(ValidationReferenceJob job) {
            return new ReferenceMetadata(
                    job.getId(),
                    job.getSourceName(),
                    job.getSourceFileHash(),
                    job.getIndexStorageUri()
            );
        }
    }
}
