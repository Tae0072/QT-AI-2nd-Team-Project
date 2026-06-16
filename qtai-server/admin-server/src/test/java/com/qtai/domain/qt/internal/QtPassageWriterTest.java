package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QtPassageWriterTest {

    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private QtPassageVerseRepository qtPassageVerseRepository;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-16T00:02:00Z"), ZoneOffset.UTC);

    private QtPassageWriter writer;

    @BeforeEach
    void setUp() {
        writer = new QtPassageWriter(qtPassageRepository, qtPassageVerseRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("미리 모은 절을 순서대로 매핑 저장한다")
    void replaceMappings_savesInOrder() {
        boolean replaced = writer.replaceMappings(100L, List.of(
                verse(902L, 9, 2), verse(903L, 9, 3), verse(1001L, 10, 1), verse(1002L, 10, 2)));

        assertThat(replaced).isTrue();
        verify(qtPassageVerseRepository).deleteByQtPassageId(100L);
        verify(qtPassageVerseRepository).saveAll(org.mockito.ArgumentMatchers.argThat(mappings -> {
            assertThat(mappings)
                    .extracting(QtPassageVerse::getBibleVerseId)
                    .containsExactly(902L, 903L, 1001L, 1002L);
            assertThat(mappings)
                    .extracting(QtPassageVerse::getDisplayOrder)
                    .containsExactly((short) 1, (short) 2, (short) 3, (short) 4);
            return true;
        }));
    }

    @Test
    @DisplayName("절 목록이 비면 매핑을 건드리지 않고 false를 반환한다")
    void replaceMappings_emptyDoesNothing() {
        boolean replaced = writer.replaceMappings(100L, List.of());

        assertThat(replaced).isFalse();
        verify(qtPassageVerseRepository, never()).deleteByQtPassageId(anyLong());
        verify(qtPassageVerseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("같은 날짜 본문이 없으면 시작/종료 장을 담아 새 본문을 저장한다")
    void upsert_createsNewPassageWhenAbsent() {
        when(qtPassageRepository.findByQtDate(any(LocalDate.class))).thenReturn(Optional.empty());
        when(qtPassageRepository.save(any(QtPassage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QtPassage saved = writer.upsert(
                LocalDate.of(2026, 6, 16), (short) 46, passage((short) 9, (short) 10, (short) 1, (short) 5));

        assertThat(saved.getChapter()).isEqualTo((short) 9);
        assertThat(saved.getEndChapter()).isEqualTo((short) 10);
        assertThat(saved.getEndBookId()).isEqualTo((short) 46);
        // 수집 시각은 기록하되, 신규 수집 본문은 미게시(PENDING_REVIEW)라 게시 시각은 비워 둔다.
        assertThat(saved.getCollectedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 0, 2));
        assertThat(saved.getStatus()).isEqualTo(QtPassageStatus.PENDING_REVIEW);
        assertThat(saved.getPublishedAt()).isNull();
        verify(qtPassageRepository).save(any(QtPassage.class));
    }

    private static SuTodayPassage passage(short chapter, short endChapter, short startVerse, short endVerse) {
        return new SuTodayPassage(
                "오늘의 QT", "고린도전서", "1 Corinthians",
                chapter, endChapter, startVerse, endVerse,
                "고린도전서(1 Corinthians) " + chapter + ":" + startVerse + "-" + endChapter + ":" + endVerse);
    }

    private static BibleVerseResponse verse(long id, int chapter, int verse) {
        return new BibleVerseResponse(id, "1CO", chapter, verse, "본문", "Text");
    }
}
