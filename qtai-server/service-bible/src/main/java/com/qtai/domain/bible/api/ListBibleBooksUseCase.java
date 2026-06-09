package com.qtai.domain.bible.api;

import com.qtai.domain.bible.api.dto.BibleBookResponse;

import java.util.List;

public interface ListBibleBooksUseCase {

    List<BibleBookResponse> listBibleBooks();
}
