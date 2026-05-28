package com.qtai.domain.study.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.GetQtSimulatorUseCase;
import com.qtai.domain.study.api.GetQtStudyContentUseCase;
import com.qtai.domain.study.api.dto.QtSimulatorResponse;
import com.qtai.domain.study.api.dto.QtStudyContentResponse;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QtStudyContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class QtStudyContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetQtStudyContentUseCase getQtStudyContentUseCase;

    @MockBean
    private GetQtSimulatorUseCase getQtSimulatorUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/qt/{id}/study-content — 200 + 공통 envelope")
    void getStudyContent_200() throws Exception {
        when(getQtStudyContentUseCase.getStudyContent(10L)).thenReturn(new QtStudyContentResponse(
                "safe summary",
                List.of(new QtStudyContentResponse.ExplanationItem(
                        1L,
                        "verse summary",
                        "verified explanation",
                        "QT-AI verified content",
                        100L
                )),
                List.of(new QtStudyContentResponse.GlossaryTermItem(
                        20L,
                        1L,
                        "sample term",
                        "sample meaning",
                        "QT-AI verified content"
                ))
        ));

        mockMvc.perform(get("/api/v1/qt/10/study-content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("safe summary"))
                .andExpect(jsonPath("$.data.explanations[0].verseId").value(1))
                .andExpect(jsonPath("$.data.explanations[0].sourceLabel").value("QT-AI verified content"))
                .andExpect(jsonPath("$.data.glossaryTerms[0].term").value("sample term"));
    }

    @Test
    @DisplayName("GET /api/v1/qt/{id}/simulator — READY 응답은 clip payload를 반환")
    void getSimulator_ready_200() throws Exception {
        when(getQtSimulatorUseCase.getSimulator(10L)).thenReturn(new QtSimulatorResponse(
                "READY",
                55L,
                10L,
                "safe clip",
                "2026.05.1",
                objectMapper.readTree("{\"scenes\":[]}"),
                "APPROVED"
        ));

        mockMvc.perform(get("/api/v1/qt/10/simulator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.clipId").value(55))
                .andExpect(jsonPath("$.data.sceneScriptJson.scenes").isArray())
                .andExpect(jsonPath("$.data.clipStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /api/v1/qt/{id}/simulator — MISSING 응답은 sceneScriptJson을 반환하지 않는다")
    void getSimulator_missing_200() throws Exception {
        when(getQtSimulatorUseCase.getSimulator(10L)).thenReturn(QtSimulatorResponse.missing(10L));

        mockMvc.perform(get("/api/v1/qt/10/simulator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MISSING"))
                .andExpect(jsonPath("$.data.sceneScriptJson").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/qt/{id}/simulator-clips/{clipId} — 스펙 경로 READY 응답")
    void getSimulatorClip_ready_200() throws Exception {
        when(getQtSimulatorUseCase.getSimulatorClip(10L, 55L)).thenReturn(new QtSimulatorResponse(
                "READY",
                55L,
                10L,
                "safe clip",
                "2026.05.1",
                objectMapper.readTree("{\"scenes\":[]}"),
                "APPROVED"
        ));

        mockMvc.perform(get("/api/v1/qt/10/simulator-clips/55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.clipId").value(55))
                .andExpect(jsonPath("$.data.sceneScriptJson.scenes").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/qt/{id}/simulator-clips/{clipId} — 비 READY 응답은 sceneScriptJson을 반환하지 않는다")
    void getSimulatorClip_missing_200() throws Exception {
        when(getQtSimulatorUseCase.getSimulatorClip(10L, 55L)).thenReturn(QtSimulatorResponse.missing(10L));

        mockMvc.perform(get("/api/v1/qt/10/simulator-clips/55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MISSING"))
                .andExpect(jsonPath("$.data.sceneScriptJson").doesNotExist());
    }

    @Test
    @DisplayName("qtPassageId가 1보다 작으면 400 INVALID_INPUT")
    void getStudyContent_invalidId_400() throws Exception {
        when(getQtStudyContentUseCase.getStudyContent(eq(0L)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT));

        mockMvc.perform(get("/api/v1/qt/0/study-content"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    @DisplayName("미존재 QT 본문이면 404 QT_PASSAGE_NOT_FOUND")
    void getStudyContent_notFound_404() throws Exception {
        when(getQtStudyContentUseCase.getStudyContent(eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/qt/999/study-content"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("Q0001"));
    }
}
