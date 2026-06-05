package com.qtai.domain.bible.web;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.common.exception.ErrorCode;
import com.qtai.security.SecurityConfig;
import com.qtai.security.JwtProvider;
import com.qtai.security.SecurityErrorResponseWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BibleController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class BibleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ListBibleBooksUseCase listBibleBooksUseCase;

    @MockBean
    private GetBibleVerseUseCase getBibleVerseUseCase;

    @Test
    @DisplayName("인증이 없으면 성경 API 접근은 401을 반환한다")
    void listBooks_whenAnonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/bible/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("관리자 권한이 없으면 관리자 API 접근은 403 envelope를 반환한다")
    void adminApi_whenUserRole_returnsForbiddenEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/unknown"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.FORBIDDEN.getMessage()))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    @WithMockUser
    @DisplayName("인증 사용자는 성경 API에 접근할 수 있다")
    void listBooks_whenAuthenticated_returnsOk() throws Exception {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bible/books"))
                .andExpect(status().isOk());
    }
}
