package com.qtai.domain.ai.client.qt;

import org.springframework.stereotype.Component;

import com.qtai.domain.ai.client.qt.dto.QtContextResult;

@Component
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
                "QT passage metadata and approved commentary context only"
        );
    }
}
