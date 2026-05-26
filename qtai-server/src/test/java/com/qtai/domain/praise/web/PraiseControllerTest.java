package com.qtai.domain.praise.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.SaveMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PraiseController MockMvc 슬라이스 테스트.
 */
@WebMvcTest(PraiseController.class)
@AutoConfigureMockMvc(addFilters = false)
class PraiseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CreatePraiseUseCase createPraiseUseCase;

    @MockBean
    private ListPraiseUseCase listPraiseUseCase;

    @MockBean
    private SaveMemberPraiseSongUseCase saveMemberPraiseSongUseCase;

    @MockBean
    private ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPraiseSongs_200_큐레이션_목록() throws Exception {
        PraiseResponse song = new PraiseResponse(
                1L, "Amazing Grace", "John Newton", "CURATED", "ACTIVE",
                LocalDateTime.of(2026, 5, 1, 0, 0));
        Page<PraiseResponse> page = new PageImpl<>(List.of(song));
        when(listPraiseUseCase.listActive(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/praise-songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Amazing Grace"));
    }

    @Test
    void createPraiseSong_201_ADMIN_등록() throws Exception {
        PraiseCreateRequest request = new PraiseCreateRequest("New Song", "Artist", null);
        PraiseResponse response = new PraiseResponse(
                2L, "New Song", "Artist", "CURATED", "ACTIVE",
                LocalDateTime.of(2026, 5, 26, 12, 0));
        when(createPraiseUseCase.create(eq(1L), any(PraiseCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/praise-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("New Song"));
    }

    @Test
    void savePraiseSong_201_내_찬양_저장() throws Exception {
        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(1L, null, "My Song");
        MemberPraiseSongResponse response = new MemberPraiseSongResponse(
                10L, 1L, "My Song", "Original", "Artist", "CURATED",
                null, LocalDateTime.of(2026, 5, 26, 12, 0));
        when(saveMemberPraiseSongUseCase.save(eq(1L), any(MemberPraiseSongCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/me/praise-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.displayTitle").value("My Song"));
    }

    @Test
    void removePraiseSong_204_삭제() throws Exception {
        mockMvc.perform(delete("/api/v1/me/praise-songs/10"))
                .andExpect(status().isNoContent());

        verify(saveMemberPraiseSongUseCase).remove(1L, 10L);
    }
}
