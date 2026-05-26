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
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;

class SystemAiGenerationJobControllerTest {

    private CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createAiGenerationJobUseCase = org.mockito.Mockito.mock(CreateAiGenerationJobUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-22T01:30:00Z"), ZoneId.of("Asia/Seoul"));
        SystemAiGenerationJobController controller = new SystemAiGenerationJobController(
                createAiGenerationJobUseCase,
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
    void roleSystemBatchRequestMapsExplanationAndReturnsAccepted() throws Exception {
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenReturn(new CreateAiGenerationJobResult(101L, "QUEUED"));

        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .header("X-Requested-By", "FORGED_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobType": "DAILY_QT_EXPLANATION",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generationJobId").value(101))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-22T10:30:00+09:00"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

        ArgumentCaptor<CreateAiGenerationJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAiGenerationJobCommand.class);
        verify(createAiGenerationJobUseCase).createAiGenerationJob(commandCaptor.capture());
        CreateAiGenerationJobCommand command = commandCaptor.getValue();
        assertThat(command.jobType()).isEqualTo("EXPLANATION");
        assertThat(command.targetType()).isEqualTo("QT_PASSAGE");
        assertThat(command.targetId()).isEqualTo(35L);
        assertThat(command.promptVersionId()).isEqualTo(3L);
        assertThat(command.requestedBy()).isEqualTo("SYSTEM_BATCH");
        assertThat(command.requestedAt()).isEqualTo(OffsetDateTime.parse("2026-05-22T10:30:00+09:00"));
    }

    @Test
    void systemBatchAuthorityMapsSimulatorJob() throws Exception {
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenReturn(new CreateAiGenerationJobResult(102L, "QUEUED"));

        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobType": "DAILY_QT_SIMULATOR",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generationJobId").value(102));

        ArgumentCaptor<CreateAiGenerationJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAiGenerationJobCommand.class);
        verify(createAiGenerationJobUseCase).createAiGenerationJob(commandCaptor.capture());
        assertThat(commandCaptor.getValue().jobType()).isEqualTo("SIMULATOR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUMMARY", "GLOSSARY", "UNKNOWN"})
    void unsupportedJobTypeReturnsBadRequest(String jobType) throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobType": "%s",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "promptVersionId": 3
                                }
                                """.formatted(jobType)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(createAiGenerationJobUseCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void nonQtPassageTargetTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobType": "DAILY_QT_EXPLANATION",
                                  "targetType": "BIBLE_VERSE",
                                  "targetId": 35,
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(createAiGenerationJobUseCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {
              "jobType": "DAILY_QT_EXPLANATION",
              "targetType": "QT_PASSAGE",
              "targetId": 0,
              "promptVersionId": 3
            }
            """,
            """
            {
              "jobType": "DAILY_QT_EXPLANATION",
              "targetType": "QT_PASSAGE",
              "targetId": 35,
              "promptVersionId": 0
            }
            """
    })
    void invalidRequiredFieldsReturnBadRequest(String body) throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(createAiGenerationJobUseCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));

        verify(createAiGenerationJobUseCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void insufficientAuthorityReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("member", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));

        verify(createAiGenerationJobUseCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void invalidStatusTransitionReturnsConflict() throws Exception {
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }

    private static String validRequestBody() {
        return """
                {
                  "jobType": "DAILY_QT_EXPLANATION",
                  "targetType": "QT_PASSAGE",
                  "targetId": 35,
                  "promptVersionId": 3
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
