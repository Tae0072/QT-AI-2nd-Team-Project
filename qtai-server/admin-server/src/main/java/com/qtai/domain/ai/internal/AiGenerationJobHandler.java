package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

interface AiGenerationJobHandler {

    AiGenerationJobType jobType();

    AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt);
}
