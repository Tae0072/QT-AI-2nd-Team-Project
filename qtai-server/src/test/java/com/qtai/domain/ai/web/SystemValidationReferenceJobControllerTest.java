package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

class SystemValidationReferenceJobControllerTest {

    private CreateValidationReferenceJobUseCase createUseCase;
    private GetValidationReferenceJobUseCase getUseCase;
    private ExpireValidationReferenceJobUseCase expireUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createUseCase = org.mockito.Mockito.mock(CreateValidationReferenceJobUseCase.class);
        getUseCase = org.mockito.Mockito.mock(GetValidationReferenceJobUseCase.class);
        expireUseCase = org.mockito.Mockito.mock(ExpireValidationReferenceJobUseCase.class);
        SystemValidationReferenceJobController controller = new SystemValidationReferenceJobController(
                createUseCase,
                getUseCase,
                expireUseCase
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
    void roleSystemBatchCanCreateValidationReferenceJob() throws Exception {
        when(createUseCase.createValidationReferenceJob(any(CreateValidationReferenceJobCommand.class)))
                .thenReturn(activeResponse());

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(33))
                .andExpect(jsonPath("$.data.sourceName").value("검증 참조 자료"))
                .andExpect(jsonPath("$.data.sourceFileName").value("reference-notes.pdf"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-05-29T04:00:00+09:00"))
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-28T10:00:00+09:00"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-28T10:00:00+09:00"))
                .andExpect(jsonPath("$.data.sourceFileHash").doesNotExist())
                .andExpect(jsonPath("$.data.storageUri").doesNotExist())
                .andExpect(jsonPath("$.data.indexStorageUri").doesNotExist());

        ArgumentCaptor<CreateValidationReferenceJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateValidationReferenceJobCommand.class);
        verify(createUseCase).createValidationReferenceJob(commandCaptor.capture());
        CreateValidationReferenceJobCommand command = commandCaptor.getValue();
        assertThat(command.sourceName()).isEqualTo("검증 참조 자료");
        assertThat(command.sourceFileName()).isEqualTo("reference-notes.pdf");
        assertThat(command.sourceFileHash()).isEqualTo("sha256:reference-hash");
        assertThat(command.storageUri()).isEqualTo("restricted://validation/reference.pdf");
        assertThat(command.indexStorageUri()).isEqualTo("restricted://validation/index");
        assertThat(command.expiresAt()).isEqualTo(OffsetDateTime.parse("2026-05-29T04:00:00+09:00"));
    }

    @Test
    void systemBatchAuthorityCanCreateValidationReferenceJob() throws Exception {
        when(createUseCase.createValidationReferenceJob(any(CreateValidationReferenceJobCommand.class)))
                .thenReturn(activeResponse());

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(principal("batch", "SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(33));
    }

    @Test
    void createRejectsUnauthenticatedUserAndAdminContentCreator() throws Exception {
        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(principal("member", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(principal("admin", "ROLE_ADMIN", "ADMIN_ROLE_CONTENT_CREATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));

        verify(createUseCase, never()).createValidationReferenceJob(any(CreateValidationReferenceJobCommand.class));
    }

    @Test
    void createRejectsMissingOrBlankRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceName": " ",
                                  "sourceFileName": "",
                                  "sourceFileHash": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(createUseCase, never()).createValidationReferenceJob(any(CreateValidationReferenceJobCommand.class));
    }

    @Test
    void getReturnsSingleValidationReferenceJob() throws Exception {
        when(getUseCase.getValidationReferenceJob(any(GetValidationReferenceJobQuery.class)))
                .thenReturn(activeResponse());

        mockMvc.perform(get("/api/v1/system/validation-reference-jobs/{jobId}", 33L)
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(33))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.sourceFileHash").doesNotExist())
                .andExpect(jsonPath("$.data.storageUri").doesNotExist())
                .andExpect(jsonPath("$.data.indexStorageUri").doesNotExist());

        ArgumentCaptor<GetValidationReferenceJobQuery> queryCaptor =
                ArgumentCaptor.forClass(GetValidationReferenceJobQuery.class);
        verify(getUseCase).getValidationReferenceJob(queryCaptor.capture());
        assertThat(queryCaptor.getValue().jobId()).isEqualTo(33L);
    }

    @Test
    void expireReturnsExpiredValidationReferenceJob() throws Exception {
        when(expireUseCase.expireValidationReferenceJob(any(ExpireValidationReferenceJobCommand.class)))
                .thenReturn(expiredResponse());

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs/{jobId}/expire", 33L)
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(33))
                .andExpect(jsonPath("$.data.status").value("EXPIRED"))
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist());

        ArgumentCaptor<ExpireValidationReferenceJobCommand> commandCaptor =
                ArgumentCaptor.forClass(ExpireValidationReferenceJobCommand.class);
        verify(expireUseCase).expireValidationReferenceJob(commandCaptor.capture());
        assertThat(commandCaptor.getValue().jobId()).isEqualTo(33L);
    }

    @Test
    void notFoundAndInvalidStatusTransitionUseExpectedHttpStatuses() throws Exception {
        when(getUseCase.getValidationReferenceJob(any(GetValidationReferenceJobQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_REFERENCE_JOB_NOT_FOUND));
        when(expireUseCase.expireValidationReferenceJob(any(ExpireValidationReferenceJobCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(get("/api/v1/system/validation-reference-jobs/{jobId}", 404L)
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("A0005"));

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs/{jobId}/expire", 33L)
                        .principal(principal("batch", "ROLE_SYSTEM_BATCH")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }

    private static String validRequestBody() {
        return """
                {
                  "sourceName": "검증 참조 자료",
                  "sourceFileName": "reference-notes.pdf",
                  "sourceFileHash": "sha256:reference-hash",
                  "storageUri": "restricted://validation/reference.pdf",
                  "indexStorageUri": "restricted://validation/index",
                  "expiresAt": "2026-05-29T04:00:00+09:00"
                }
                """;
    }

    private static ValidationReferenceJobResponse activeResponse() {
        return new ValidationReferenceJobResponse(
                33L,
                "검증 참조 자료",
                "reference-notes.pdf",
                "ACTIVE",
                OffsetDateTime.parse("2026-05-29T04:00:00+09:00"),
                null,
                OffsetDateTime.parse("2026-05-28T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-28T10:00:00+09:00")
        );
    }

    private static ValidationReferenceJobResponse expiredResponse() {
        return new ValidationReferenceJobResponse(
                33L,
                "검증 참조 자료",
                "reference-notes.pdf",
                "EXPIRED",
                OffsetDateTime.parse("2026-05-29T04:00:00+09:00"),
                null,
                OffsetDateTime.parse("2026-05-28T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-28T11:00:00+09:00")
        );
    }

    private static Authentication principal(Object principal, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, "N/A", grantedAuthorities);
    }
}
