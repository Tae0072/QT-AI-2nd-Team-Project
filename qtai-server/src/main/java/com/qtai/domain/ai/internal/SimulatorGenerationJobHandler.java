package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Component
class SimulatorGenerationJobHandler implements AiGenerationJobHandler {

    @Override
    public AiGenerationJobType jobType() {
        return AiGenerationJobType.SIMULATOR;
    }

    @Override
    public AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "SIMULATOR_GENERATION_DISABLED");
    }
}
