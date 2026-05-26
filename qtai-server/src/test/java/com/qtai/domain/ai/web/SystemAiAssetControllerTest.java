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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetResult;

class SystemAiAssetControllerTest {

    private RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registerAiGeneratedAssetUseCase = org.mockito.Mockito.mock(RegisterAiGeneratedAssetUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T01:30:00Z"), ZoneId.of("Asia/Seoul"));
        SystemAiAssetController controller = new SystemAiAssetController(registerAiGeneratedAssetUseCase, clock);
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setMessageConverters(jsonConverter)
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleSystemBatchRequestMapsPayloadJsonAndReturnsAccepted() throws Exception {
        when(registerAiGeneratedAssetUseCase.registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class)))
                .thenReturn(new RegisterAiGeneratedAssetResult(500L, "VALIDATING"));

        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .header("X-Requested-By", "FORGED_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "generationJobId": 101,
                                  "assetType": "EXPLANATION",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "payloadJson": {
                                    "summary": "검증 대기 해설",
                                    "sources": [{"label": "curated"}]
                                  },
                                  "sourceLabel": "QT-AI curated content",
                                  "status": "VALIDATING"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assetId").value(500))
                .andExpect(jsonPath("$.data.status").value("VALIDATING"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

        ArgumentCaptor<RegisterAiGeneratedAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterAiGeneratedAssetCommand.class);
        verify(registerAiGeneratedAssetUseCase).registerAiGeneratedAsset(commandCaptor.capture());
        RegisterAiGeneratedAssetCommand command = commandCaptor.getValue();
        assertThat(command.generationJobId()).isEqualTo(101L);
        assertThat(command.assetType()).isEqualTo("EXPLANATION");
        assertThat(command.targetType()).isEqualTo("QT_PASSAGE");
        assertThat(command.targetId()).isEqualTo(35L);
        assertThat(command.payloadJson())
                .isEqualTo("{\"summary\":\"검증 대기 해설\",\"sources\":[{\"label\":\"curated\"}]}");
        assertThat(command.sourceLabel()).isEqualTo("QT-AI curated content");
        assertThat(command.createdAt()).isEqualTo(OffsetDateTime.parse("2026-05-26T10:30:00+09:00"));
    }

    @Test
    void systemBatchAuthorityAllowsRequestWithoutStatus() throws Exception {
        when(registerAiGeneratedAssetUseCase.registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class)))
                .thenReturn(new RegisterAiGeneratedAssetResult(501L, "VALIDATING"));

        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("batch", "SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutStatus()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.assetId").value(501))
                .andExpect(jsonPath("$.data.status").value("VALIDATING"));

        verify(registerAiGeneratedAssetUseCase).registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED", "HIDDEN"})
    void statusOtherThanValidatingReturnsBadRequest(String statusValue) throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithStatus(statusValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(registerAiGeneratedAssetUseCase, never())
                .registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutStatus()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));

        verify(registerAiGeneratedAssetUseCase, never())
                .registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class));
    }

    @Test
    void insufficientAuthorityReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("member", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutStatus()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));

        verify(registerAiGeneratedAssetUseCase, never())
                .registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class));
    }

    @Test
    void missingRequiredFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType": "EXPLANATION",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "payloadJson": {"summary": "검증 대기 해설"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(registerAiGeneratedAssetUseCase, never())
                .registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class));
    }

    @Test
    void generationJobNotFoundReturnsNotFound() throws Exception {
        when(registerAiGeneratedAssetUseCase.registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.AI_GENERATION_JOB_NOT_FOUND));

        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutStatus()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A0001"));
    }

    private static String validRequestBodyWithoutStatus() {
        return """
                {
                  "generationJobId": 101,
                  "assetType": "EXPLANATION",
                  "targetType": "QT_PASSAGE",
                  "targetId": 35,
                  "payloadJson": {"summary": "검증 대기 해설"},
                  "sourceLabel": "QT-AI curated content"
                }
                """;
    }

    private static String validRequestBodyWithStatus(String statusValue) {
        return """
                {
                  "generationJobId": 101,
                  "assetType": "EXPLANATION",
                  "targetType": "QT_PASSAGE",
                  "targetId": 35,
                  "payloadJson": {"summary": "검증 대기 해설"},
                  "sourceLabel": "QT-AI curated content",
                  "status": "%s"
                }
                """.formatted(statusValue);
    }

    private static Authentication principal(Object principal, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, "N/A", grantedAuthorities);
    }
}
