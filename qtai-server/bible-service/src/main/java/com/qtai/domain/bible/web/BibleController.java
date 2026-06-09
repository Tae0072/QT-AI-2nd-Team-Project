package com.qtai.domain.bible.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bible")
@RequiredArgsConstructor
public class BibleController {

    /** 배치 verse 조회 1회 ID 상한 — 과대 요청 방어. */
    static final int MAX_VERSE_IDS = 200;

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
     *
     * <p>입력 안전성: ids는 1개 이상 {@link #MAX_VERSE_IDS}개 이하여야 한다(빈/초과 시 400 C0002).
     * <b>정책 = all-or-nothing</b> — 요청 ID 중 하나라도 존재하지 않으면 404(B0002)를 반환한다(부분 매치 아님).
     * 음수/0 ID는 400(C0002). bible-service엔 GlobalExceptionHandler가 없어 도메인 {@link BusinessException}을
     * 직접 잡아 ErrorCode 상태·표준 envelope로 변환한다(미변환 시 500 누출 방지).
     */
    @GetMapping("/verses/by-ids")
    public ResponseEntity<ApiResponse<List<BibleVerseResponse>>> getVersesByIds(
            @RequestParam(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    ErrorCode.INVALID_INPUT.getCode(), "ids는 1개 이상이어야 합니다."));
        }
        if (ids.size() > MAX_VERSE_IDS) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    ErrorCode.INVALID_INPUT.getCode(), "ids는 최대 " + MAX_VERSE_IDS + "개까지 조회할 수 있습니다."));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(getBibleVerseUseCase.getVerses(ids)));
        } catch (BusinessException e) {
            ErrorCode code = e.getErrorCode();
            return ResponseEntity.status(code.getHttpStatus())
                    .body(ApiResponse.error(code.getCode(), code.getMessage()));
        }
    }
}
