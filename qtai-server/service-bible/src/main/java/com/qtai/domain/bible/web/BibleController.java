package com.qtai.domain.bible.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * 구절 id 다건 조회 — 서비스 간 호출(예: service-note가 노트에 연결된 구절 메타를 채울 때).
     *
     * <p>리터럴 경로({@code /verses/by-ids})라 path variable {@code /verses/{verseId}}보다 우선 매칭된다.
     * 요청 id 순서를 보존하지 않을 수 있으므로 호출자는 응답의 {@code id}로 매핑한다(NoteService 참고).
     */
    @GetMapping("/verses/by-ids")
    public ApiResponse<List<BibleVerseResponse>> getVersesByIds(@RequestParam List<Long> ids) {
        return ApiResponse.success(getBibleVerseUseCase.getVerses(ids));
    }

    /** 구절 id 단건 조회 — 서비스 간 호출용. */
    @GetMapping("/verses/{verseId}")
    public ApiResponse<BibleVerseResponse> getVerse(@PathVariable Long verseId) {
        return ApiResponse.success(getBibleVerseUseCase.getVerse(verseId));
    }
}
