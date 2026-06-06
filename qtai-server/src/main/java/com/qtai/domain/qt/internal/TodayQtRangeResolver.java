package com.qtai.domain.qt.internal;

import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TodayQtRangeResolver {

    private final BibleBookLookup bibleBookLookup;

    /**
     * QT 본문의 장/절(qt 소유)과 권 메타(bible api 조회)를 합쳐 범위 응답을 만든다(리뷰 §5.2 #1).
     * 권을 못 찾으면 null을 반환해 호출부가 범위 없이 처리한다.
     */
    TodayQtRangeResponse resolve(QtPassage passage) {
        return bibleBookLookup.findById(passage.getBookId())
                .map(book -> TodayQtRangeMapper.toResponse(book, passage))
                .orElse(null);
    }
}
