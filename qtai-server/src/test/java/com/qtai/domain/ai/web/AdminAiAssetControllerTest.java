package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;

class AdminAiAssetControllerTest {

    private RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        regenerateAiAssetUseCase = org.mockito.Mockito.mock(RegenerateAiAssetUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-21T01:30:00Z"), ZoneId.of("Asia/Seoul"));
        AdminAiAssetController controller = new AdminAiAssetController(regenerateAiAssetUseCase, clock);
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter)
                .build();
    }

    @Test
    void regenerateMapsRequestAndReturnsAcceptedQueuedJob() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        101L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .header("X-Admin-Id", "7")
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다.",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generationJobId").value(101))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-21T10:30:00+09:00"));

        ArgumentCaptor<RegenerateAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(RegenerateAiAssetCommand.class);
        verify(regenerateAiAssetUseCase).regenerateAiAsset(commandCaptor.capture());
        RegenerateAiAssetCommand command = commandCaptor.getValue();
        assertThat(command.adminId()).isEqualTo(7L);
        assertThat(command.assetId()).isEqualTo(500L);
        assertThat(command.memberRole()).isEqualTo("ADMIN");
        assertThat(command.adminRole()).isEqualTo("REVIEWER");
        assertThat(command.promptVersionId()).isEqualTo(3L);
        assertThat(command.requestedAt())
                .isEqualTo(OffsetDateTime.parse("2026-05-21T10:30:00+09:00"));
    }

    @Test
    void forbiddenAdminRoleReturnsForbiddenResponse() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .header("X-Admin-Id", "7")
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "OPERATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검토 권한 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    @Test
    void invalidAssetStatusReturnsConflictResponse() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .header("X-Admin-Id", "7")
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검증 중 상태 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }
}
