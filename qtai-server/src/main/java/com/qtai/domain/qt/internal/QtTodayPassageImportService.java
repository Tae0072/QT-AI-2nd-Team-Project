package com.qtai.domain.qt.internal;

import java.time.LocalDate;

import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QtTodayPassageImportService {

    private final QtPassageRepository qtPassageRepository;

    @Transactional
    @CacheEvict(cacheNames = "todayQt", allEntries = true)
    public QtPassage importToday(LocalDate qtDate, SuTodayPassage passage) {
        Short bookId = qtPassageRepository.findBookIdByEnglishName(passage.englishBookName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "DB에 등록되지 않은 성경 권입니다. englishName=" + passage.englishBookName()));

        QtPassage qtPassage = qtPassageRepository.findByQtDate(qtDate)
                .orElseGet(() -> QtPassage.create(
                        qtDate,
                        bookId,
                        passage.chapter(),
                        passage.startVerse(),
                        passage.endVerse(),
                        passage.title(),
                        passage.referenceText()
                ));
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
