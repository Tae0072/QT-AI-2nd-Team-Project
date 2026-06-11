package com.qtai.external.bible;

import java.util.List;

import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;

/**
 * {@link ListBibleBooksUseCase}의 HTTP 어댑터 (mode=http일 때 {@code @Primary}로 등록).
 * 모놀리식 소비자(qt 등)의 인터페이스 의존을 바꾸지 않고 호출만 bible-service HTTP로 우회한다.
 */
public class ListBibleBooksHttpAdapter implements ListBibleBooksUseCase {

    private final BibleServiceClient client;

    public ListBibleBooksHttpAdapter(BibleServiceClient client) {
        this.client = client;
    }

    @Override
    public List<BibleBookResponse> listBibleBooks() {
        return client.listBibleBooks();
    }
}
