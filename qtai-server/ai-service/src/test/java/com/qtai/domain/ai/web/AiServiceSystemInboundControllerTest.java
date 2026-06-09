package com.qtai.domain.ai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetResult;
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogResult;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

class AiServiceSystemInboundControllerTest {

    @Test
    void generationJobAcceptsSystemBatchRequest() throws Exception {
        CreateAiGenerationJobUseCase useCase = org.mockito.Mockito.mock(CreateAiGenerationJobUseCase.class);
        when(useCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenReturn(new CreateAiGenerationJobResult(101L, "QUEUED"));

        mockMvc(new SystemAiGenerationJobController(useCase))
                .perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(systemPrincipal("ROLE_SYSTEM_BATCH"))
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
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        verify(useCase).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void generationJobRejectsMissingAndWrongSystemAuthority() throws Exception {
        CreateAiGenerationJobUseCase useCase = org.mockito.Mockito.mock(CreateAiGenerationJobUseCase.class);
        MockMvc mockMvc = mockMvc(new SystemAiGenerationJobController(useCase));

        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGenerationJobBody()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/system/ai/generation-jobs")
                        .principal(systemPrincipal("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGenerationJobBody()))
                .andExpect(status().isForbidden());

        verify(useCase, never()).createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void assetValidationLogAndValidationReferenceEndpointsReturnSuccess() throws Exception {
        RegisterAiGeneratedAssetUseCase assetUseCase = org.mockito.Mockito.mock(RegisterAiGeneratedAssetUseCase.class);
        RegisterAiValidationLogUseCase validationLogUseCase =
                org.mockito.Mockito.mock(RegisterAiValidationLogUseCase.class);
        CreateValidationReferenceJobUseCase createReferenceUseCase =
                org.mockito.Mockito.mock(CreateValidationReferenceJobUseCase.class);
        GetValidationReferenceJobUseCase getReferenceUseCase =
                org.mockito.Mockito.mock(GetValidationReferenceJobUseCase.class);
        ExpireValidationReferenceJobUseCase expireReferenceUseCase =
                org.mockito.Mockito.mock(ExpireValidationReferenceJobUseCase.class);

        when(assetUseCase.registerAiGeneratedAsset(any(RegisterAiGeneratedAssetCommand.class)))
                .thenReturn(new RegisterAiGeneratedAssetResult(501L, "VALIDATING"));
        when(validationLogUseCase.registerAiValidationLog(any(RegisterAiValidationLogCommand.class)))
                .thenReturn(new RegisterAiValidationLogResult(701L, "PASS", "APPROVED"));
        when(createReferenceUseCase.createValidationReferenceJob(any(CreateValidationReferenceJobCommand.class)))
                .thenReturn(referenceResponse(901L, "READY"));
        when(getReferenceUseCase.getValidationReferenceJob(any(GetValidationReferenceJobQuery.class)))
                .thenReturn(referenceResponse(901L, "READY"));
        when(expireReferenceUseCase.expireValidationReferenceJob(any(ExpireValidationReferenceJobCommand.class)))
                .thenReturn(referenceResponse(901L, "EXPIRED"));

        MockMvc mockMvc = mockMvc(
                new SystemAiAssetController(assetUseCase),
                new SystemAiValidationLogController(validationLogUseCase),
                new SystemValidationReferenceJobController(
                        createReferenceUseCase,
                        getReferenceUseCase,
                        expireReferenceUseCase
                )
        );

        mockMvc.perform(post("/api/v1/system/ai/assets")
                        .principal(systemPrincipal("SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "generationJobId": 101,
                                  "assetType": "EXPLANATION",
                                  "targetType": "QT_PASSAGE",
                                  "targetId": 35,
                                  "payloadJson": {"summary": "allowed summary"},
                                  "sourceLabel": "QT-AI",
                                  "status": "VALIDATING"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.assetId").value(501))
                .andExpect(jsonPath("$.data.status").value("VALIDATING"));

        mockMvc.perform(post("/api/v1/system/ai/validation-logs")
                        .principal(systemPrincipal("SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aiAssetId": 501,
                                  "validationReferenceJobId": 901,
                                  "checklistVersionId": 12,
                                  "layer": 1,
                                  "result": "PASS",
                                  "checklistJson": {"items": []},
                                  "reviewerType": "SYSTEM_BATCH"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.validationLogId").value(701))
                .andExpect(jsonPath("$.data.result").value("PASS"));

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .principal(systemPrincipal("SYSTEM_BATCH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceName": "review-reference",
                                  "sourceFileName": "reference.pdf",
                                  "sourceFileHash": "hash-1",
                                  "storageUri": "restricted://reference/source",
                                  "indexStorageUri": "restricted://reference/index",
                                  "expiresAt": "2026-06-10T00:00:00+09:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(901));

        mockMvc.perform(get("/api/v1/system/validation-reference-jobs/901")
                        .principal(systemPrincipal("SYSTEM_BATCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        mockMvc.perform(post("/api/v1/system/validation-reference-jobs/901/expire")
                        .principal(systemPrincipal("SYSTEM_BATCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));
    }

    private static MockMvc mockMvc(Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controllers)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()
                ))
                .build();
    }

    private static Authentication systemPrincipal(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "batch",
                "n/a",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    private static String validGenerationJobBody() {
        return """
                {
                  "jobType": "DAILY_QT_EXPLANATION",
                  "targetType": "QT_PASSAGE",
                  "targetId": 35,
                  "promptVersionId": 3
                }
                """;
    }

    private static ValidationReferenceJobResponse referenceResponse(Long id, String status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-09T00:00:00+09:00");
        return new ValidationReferenceJobResponse(
                id,
                "review-reference",
                "reference.pdf",
                status,
                now.plusDays(1),
                null,
                now,
                now
        );
    }
}
