package com.qtai.external.bible;

import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 단건 getVerse(Long)가 배치({@code /verses/by-ids}) 경로로 위임되는지 검증.
 */
class GetBibleVerseHttpAdapterTest {

    private final BibleServiceClient client = mock(BibleServiceClient.class);
    private final GetBibleVerseHttpAdapter adapter = new GetBibleVerseHttpAdapter(client);

    @Test
    void getVerse_delegatesToByIds_returnsFirst() {
        BibleVerseResponse verse = new BibleVerseResponse(10L, "GEN", 1, 1, "k", "e");
        when(client.getVerses(List.of(10L))).thenReturn(List.of(verse));

        assertThat(adapter.getVerse(10L)).isEqualTo(verse);
    }

    @Test
    void getVerse_emptyResult_throwsNotFound() {
        when(client.getVerses(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> adapter.getVerse(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND);
    }
}
