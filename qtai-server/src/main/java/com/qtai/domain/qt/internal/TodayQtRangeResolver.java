package com.qtai.domain.qt.internal;

import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TodayQtRangeResolver {

    private final QtPassageRepository qtPassageRepository;

    TodayQtRangeResponse resolve(QtPassage passage) {
        return qtPassageRepository.findRangeByQtPassageId(passage.getId())
                .map(TodayQtRangeMapper::toResponse)
                .orElse(null);
    }
}
