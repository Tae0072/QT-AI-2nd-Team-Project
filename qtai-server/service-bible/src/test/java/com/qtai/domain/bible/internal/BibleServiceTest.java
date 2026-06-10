package com.qtai.domain.bible.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BibleServiceTest {

    @Mock
    private BibleBookRepository bibleBookRepository;

    @Mock
    private BibleRepository bibleRepository;

    @InjectMocks
    private BibleService bibleService;

    @Test
    void getVerse_널이거나_1미만이면_INVALID_INPUT() {
        BusinessException nullEx = assertThrows(BusinessException.class, () -> bibleService.getVerse(null));
        assertEquals(ErrorCode.INVALID_INPUT, nullEx.getErrorCode());

        BusinessException zeroEx = assertThrows(BusinessException.class, () -> bibleService.getVerse(0L));
        assertEquals(ErrorCode.INVALID_INPUT, zeroEx.getErrorCode());
    }

    @Test
    void getVerse_없는_절이면_BIBLE_VERSE_NOT_FOUND() {
        when(bibleRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> bibleService.getVerse(99L));
        assertEquals(ErrorCode.BIBLE_VERSE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getVerses_빈_목록이면_빈_결과() {
        assertTrue(bibleService.getVerses(List.of()).isEmpty());
        assertTrue(bibleService.getVerses((List<Long>) null).isEmpty());
    }

    @Test
    void getVerses_잘못된_id_포함이면_INVALID_INPUT() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bibleService.getVerses(List.of(1L, 0L)));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void getVersesRange_없는_책이면_BIBLE_BOOK_NOT_FOUND() {
        when(bibleBookRepository.findByCode("GEN")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bibleService.getVerses("GEN", 1, null, null));
        assertEquals(ErrorCode.BIBLE_BOOK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getVersesRange_잘못된_장이면_INVALID_INPUT() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bibleService.getVerses("GEN", 0, null, null));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }
}
