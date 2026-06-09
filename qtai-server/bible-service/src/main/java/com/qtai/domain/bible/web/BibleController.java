package com.qtai.domain.bible.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bible")
@RequiredArgsConstructor
public class BibleController {

    private final ListBibleBooksUseCase listBibleBooksUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;

    @GetMapping("/books")
    public ApiResponse<List<BibleBookResponse>> listBooks() {
        return ApiResponse.success(listBibleBooksUseCase.listBibleBooks());
    }

    @GetMapping("/verses")
    public ApiResponse<BibleVerseRangeResponse> getVerses(
            @RequestParam String bookCode,
            @RequestParam int chapter,
            @RequestParam(required = false) Integer verseFrom,
            @RequestParam(required = false) Integer verseTo
    ) {
        return ApiResponse.success(getBibleVerseUseCase.getVerses(bookCode, chapter, verseFrom, verseTo));
    }

    /**
     * verse ID 배치 조회. note 등 소비자가 노트에 연결된 절들을 ID로 일괄 조회하는 경로
     * (MSA 분리 Inc3 — 소비자 HTTP 어댑터 전환 시 사용). 읽기 전용·멱등이라 GET + ids 쿼리.
     */
    @GetMapping("/verses/by-ids")
    public ApiResponse<List<BibleVerseResponse>> getVersesByIds(@RequestParam List<Long> ids) {
        return ApiResponse.success(getBibleVerseUseCase.getVerses(ids));
    }
}
