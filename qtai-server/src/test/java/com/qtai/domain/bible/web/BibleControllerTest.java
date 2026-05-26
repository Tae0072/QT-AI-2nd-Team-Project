package com.qtai.domain.bible.web;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BibleControllerTest {

    private final ListBibleBooksUseCase listBibleBooksUseCase = mock(ListBibleBooksUseCase.class);
    private final GetBibleVerseUseCase getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BibleController controller = new BibleController(listBibleBooksUseCase, getBibleVerseUseCase);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/bible/books는 공통 envelope로 성경 권 목록을 반환한다")
    void listBooks_returnsEnvelope() throws Exception {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(new BibleBookResponse(
                (short) 1,
                "OLD",
                "GEN",
                "창세기",
                "Genesis",
                (short) 1
        )));

        mockMvc.perform(get("/api/v1/bible/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("GEN"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/bible/verses는 query parameter를 UseCase에 위임한다")
    void getVerses_delegatesQueryParameters() throws Exception {
        when(getBibleVerseUseCase.getVerses("GEN", 1, 2, null)).thenReturn(new BibleVerseRangeResponse(
                new BibleVerseBookResponse("GEN", "창세기", "Genesis", 1),
                List.of(new BibleVerseResponse(10L, "GEN", 1, 2, "테스트 본문", "Test text"))
        ));

        mockMvc.perform(get("/api/v1/bible/verses")
                        .param("bookCode", "GEN")
                        .param("chapter", "1")
                        .param("verseFrom", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.book.code").value("GEN"))
                .andExpect(jsonPath("$.data.verses[0].verseNo").value(2));
    }

    @Test
    @DisplayName("UseCase 예외는 공통 에러 envelope로 반환한다")
    void getVerses_whenUseCaseThrows_returnsErrorEnvelope() throws Exception {
        when(getBibleVerseUseCase.getVerses("NONE", 1, null, null))
                .thenThrow(new BusinessException(ErrorCode.BIBLE_BOOK_NOT_FOUND));

        mockMvc.perform(get("/api/v1/bible/verses")
                        .param("bookCode", "NONE")
                        .param("chapter", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("B0001"));
    }
}
