package com.qtai.external.bible;

import java.util.List;

import com.qtai.domain.bible.api.dto.BibleBookResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ListBibleBooksHttpAdapter가 클라이언트에 위임하는지 검증.
 */
class ListBibleBooksHttpAdapterTest {

    private final BibleServiceClient client = mock(BibleServiceClient.class);
    private final ListBibleBooksHttpAdapter adapter = new ListBibleBooksHttpAdapter(client);

    @Test
    void listBibleBooks_delegatesToClient() {
        BibleBookResponse book = new BibleBookResponse(1, "OLD", "GEN", "창세기", "Genesis", 1);
        when(client.listBibleBooks()).thenReturn(List.of(book));

        assertThat(adapter.listBibleBooks()).containsExactly(book);
    }
}
