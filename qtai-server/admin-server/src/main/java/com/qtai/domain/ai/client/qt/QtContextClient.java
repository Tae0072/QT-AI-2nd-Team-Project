package com.qtai.domain.ai.client.qt;

import java.time.LocalDate;

import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.qt.dto.QtContextResult;

public interface QtContextClient {

    QtContextResult getQtContext(Long viewerId, Long qtPassageId) throws AiClientException;

    TodayQtPassageStatus getTodayQtPassageStatus(LocalDate qtDate) throws AiClientException;

    record TodayQtPassageStatus(
            LocalDate qtDate,
            boolean exists,
            Long passageId,
            CacheStatus cacheStatus
    ) {
    }

    enum CacheStatus {
        HIT,
        MISS,
        STALE_FALLBACK,
        EMPTY
    }
}
