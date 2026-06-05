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
import com.qtai.domain.ai.api.validation.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogResult;

class SystemAiValidationLogControllerTest {

    private RegisterAiValidationLogUseCase registerAiValidationLogUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registerAiValidationLogUseCase = org.mockito.Mockito.mock(RegisterAiValidationLogUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T02:30:00Z"), ZoneId.of("Asia/Seoul"));
        SystemAiValidationLogController controller = new SystemAiValidationLogController(
                registerAiValidationLogUseCase,
                clock
        );
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
    void roleSystemBatchRequestMapsValidationReferenceJobIdAndReturnsAccepted() throws Exception {
        when(registerAiValidationLogUseCase.registerAiValidationLog(any(RegisterAiValidationLogCommand.class)))
                .thenReturn(new RegisterAiValidationLogResult(700L, "PASSED", "VALIDATING"));

        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aiAssetId": 500,
                                  "validationReferenceJobId": 33,
                                  "checklistVersionId": 4,
                                  "layer": 2,
                                  "result": "PASSED",
                                  "checklistJson": {
                                    "rules": [{"id": "source", "passed": true}]
                                  },
                                  "reviewerType": "AUTO",
                                  "errorMessage": null
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.validationLogId").value(700))
                .andExpect(jsonPath("$.data.result").value("PASSED"))
                .andExpect(jsonPath("$.data.assetStatus").value("VALIDATING"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

        ArgumentCaptor<RegisterAiValidationLogCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterAiValidationLogCommand.class);
        verify(registerAiValidationLogUseCase).registerAiValidationLog(commandCaptor.capture());
        RegisterAiValidationLogCommand command = commandCaptor.getValue();
        assertThat(command.assetId()).isEqualTo(500L);
        assertThat(command.validationReferenceJobId()).isEqualTo(33L);
        assertThat(command.checklistVersionId()).isEqualTo(4L);
        assertThat(command.layer()).isEqualTo(2);
        assertThat(command.result()).isEqualTo("PASSED");
        assertThat(command.reviewerType()).isEqualTo("AUTO");
        assertThat(command.checklistJson())
                .isEqualTo("{\"rules\":[{\"id\":\"source\",\"passed\":true}]}");
        assertThat(command.createdAt()).isEqualTo(OffsetDateTime.parse("2026-05-26T11:30:00+09:00"));
    }

    @Test
    void validationReferenceJobIdMayBeOmitted() throws Exception {
        when(registerAiValidationLogUseCase.registerAiValidationLog(any(RegisterAiValidationLogCommand.class)))
                .thenReturn(new RegisterAiValidationLogResult(701L, "NEEDS_REVIEW", "VALIDATING"));

        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("batch", "SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutValidationReferenceJob()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.validationLogId").value(701))
                .andExpect(jsonPath("$.data.assetStatus").value("VALIDATING"));

        ArgumentCaptor<RegisterAiValidationLogCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterAiValidationLogCommand.class);
        verify(registerAiValidationLogUseCase).registerAiValidationLog(commandCaptor.capture());
        assertThat(commandCaptor.getValue().validationReferenceJobId()).isNull();
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutValidationReferenceJob()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));

        verify(registerAiValidationLogUseCase, never())
                .registerAiValidationLog(any(RegisterAiValidationLogCommand.class));
    }

    @Test
    void insufficientAuthorityReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("member", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutValidationReferenceJob()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));

        verify(registerAiValidationLogUseCase, never())
                .registerAiValidationLog(any(RegisterAiValidationLogCommand.class));
    }

    @Test
    void missingRequiredFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistVersionId": 4,
                                  "layer": 2,
                                  "result": "PASSED",
                                  "checklistJson": {"rules": []},
                                  "reviewerType": "AUTO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(registerAiValidationLogUseCase, never())
                .registerAiValidationLog(any(RegisterAiValidationLogCommand.class));
    }

    @Test
    void assetNotFoundReturnsNotFound() throws Exception {
        when(registerAiValidationLogUseCase.registerAiValidationLog(any(RegisterAiValidationLogCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));

        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutValidationReferenceJob()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A0002"));
    }

    @Test
    void invalidStatusTransitionReturnsConflict() throws Exception {
        when(registerAiValidationLogUseCase.registerAiValidationLog(any(RegisterAiValidationLogCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBodyWithoutValidationReferenceJob()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }

    private static String validRequestBodyWithoutValidationReferenceJob() {
        return """
                {
                  "aiAssetId": 500,
                  "checklistVersionId": 4,
                  "layer": 2,
                  "result": "NEEDS_REVIEW",
                  "checklistJson": {"rules": [{"id": "tone", "passed": false}]},
                  "reviewerType": "AUTO",
                  "errorMessage": "NEEDS_HUMAN_REVIEW"
                }
                """;
    }

    private static Authentication principal(Object principal, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, "N/A", grantedAuthorities);
    }
}
