package com.qtai.domain.qt.internal;

import java.time.LocalDate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QtTodayPassageImportService {

    private final QtPassageRepository qtPassageRepository;

    @Transactional
    public QtPassage importToday(LocalDate qtDate, SuTodayPassage passage) {
        Short bookId = qtPassageRepository.findBookIdByEnglishName(passage.englishBookName())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BIBLE_BOOK_NOT_FOUND,
                        "DB에 등록되지 않은 성경 권입니다. englishName=" + passage.englishBookName()));

        return qtPassageRepository.findByQtDate(qtDate)
                .map(qtPassage -> updateExisting(qtPassage, bookId, passage))
                .orElseGet(() -> createNew(qtDate, bookId, passage));
    }

    private QtPassage createNew(LocalDate qtDate, Short bookId, SuTodayPassage passage) {
        QtPassage qtPassage = QtPassage.create(
                qtDate,
                bookId,
                passage.chapter(),
                passage.startVerse(),
                passage.endVerse(),
                passage.title(),
                passage.referenceText()
        );
        return qtPassageRepository.save(qtPassage);
    }

    private QtPassage updateExisting(QtPassage qtPassage, Short bookId, SuTodayPassage passage) {
        qtPassage.updateRange(
                bookId,
                passage.chapter(),
                passage.startVerse(),
                passage.endVerse(),
                passage.title(),
                passage.referenceText()
        );
        return qtPassageRepository.save(qtPassage);
    }
}
