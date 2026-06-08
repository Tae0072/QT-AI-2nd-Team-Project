package com.qtai.domain.ai.client.qt;

import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.qtai.domain.ai.client.qt.dto.QtContextResult;

@Component("aiGetQtUseCaseMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "mock", matchIfMissing = true)
@ConditionalOnMissingBean(QtContextClient.class)
public class GetQtUseCaseMock implements QtContextClient {

    @Override
    public QtContextResult getQtContext(Long viewerId, Long qtPassageId) {
        return new QtContextResult(
                qtPassageId,
                "JOHN",
                3,
                16,
                16,
                "John 3:16",
                "Today QT",
                "Mock summary",
                "QT passage metadata and approved commentary context only"
        );
    }

    @Override
    public TodayQtPassageStatus getTodayQtPassageStatus(LocalDate qtDate) {
        return new TodayQtPassageStatus(qtDate, true, 35L, CacheStatus.HIT);
    }
}
