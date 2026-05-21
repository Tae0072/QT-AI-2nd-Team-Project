package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.slf4j.MDC;

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

    @AfterEach
    void tearDown() {
        MDC.clear();
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
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
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
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-21T10:30:00+09:00"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

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
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검토 권한 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    @Test
    void invalidAssetStatusReturnsConflictResponse() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검증 중 상태 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0003"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void responseEnvelopeUsesMdcTraceIdWhenPresent() throws Exception {
        MDC.put("traceId", "trace-ai-regenerate-001");
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        101L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다.",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.traceId").value("trace-ai-regenerate-001"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorizedEvenWithForgedHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .header("X-Admin-Id", "7")
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "헤더 위조 시도",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    @Test
    void forgedHeadersDoNotOverrideSecurityContextRoles() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_REVIEWER"))
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "권한 헤더 위조 시도",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    private static Authentication adminPrincipal(Long adminId, String... adminAuthorities) {
        String[] authorities = new String[adminAuthorities.length + 1];
        authorities[0] = "ROLE_ADMIN";
        System.arraycopy(adminAuthorities, 0, authorities, 1, adminAuthorities.length);
        return principal(adminId, authorities);
    }

    private static Authentication principal(Long principalId, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principalId, "N/A", grantedAuthorities);
    }
}
