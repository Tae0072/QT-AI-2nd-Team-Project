package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QtServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 6, 10).atStartOfDay(KST).toInstant(), KST);

    @Mock
    private QtPassageLookup passageLookup;

    @Mock
    private QtPassageRepository qtPassageRepository;

    @Mock
    private QtPassageVerseRepository qtPassageVerseRepository;

    @Mock
    private TodayQtRangeResolver rangeResolver;

    @Mock
    private GetNoteUseCase getNoteUseCase;

    @Mock
    private GetQtStudyAvailabilityUseCase getQtStudyAvailabilityUseCase;

    private QtService qtService;

    @BeforeEach
    void setUp() {
        qtService = new QtService(
                passageLookup,
                qtPassageRepository,
                qtPassageVerseRepository,
                rangeResolver,
                getNoteUseCase,
                getQtStudyAvailabilityUseCase,
                FIXED_CLOCK
        );
    }

    @Test
    @DisplayName("getPassage blocks hidden passages")
    void getPassage_hidden_blocks() {
        when(qtPassageRepository.findById(6L))
                .thenReturn(Optional.of(hiddenPassage(6L, LocalDate.of(2026, 6, 1))));

        assertThatThrownBy(() -> qtService.getPassage(1L, 6L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("getContentContext returns hidden passage with published false")
    void getContentContext_hidden_publishedFalse() {
        when(qtPassageRepository.findById(8L))
                .thenReturn(Optional.of(hiddenPassage(8L, LocalDate.of(2026, 6, 1))));
        when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(8L))
                .thenReturn(List.of());

        var result = qtService.getContentContext(8L);

        assertThat(result.qtPassageId()).isEqualTo(8L);
        assertThat(result.published()).isFalse();
    }

    private static QtPassage hiddenPassage(Long id, LocalDate date) {
        QtPassage passage = QtPassage.create(
                date,
                (short) 19,
                (short) 23,
                (short) 1,
                (short) 6,
                "관리자 QT",
                "시 23:1-6"
        );
        ReflectionTestUtils.setField(passage, "id", id);
        passage.publish(date.atStartOfDay());
        passage.hide(date.atStartOfDay().plusHours(1));
        return passage;
    }
}
