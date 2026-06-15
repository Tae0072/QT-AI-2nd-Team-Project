package com.qtai.domain.ai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.prompt.ActivateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.CreateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.GetAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.ListAiPromptVersionsUseCase;
import com.qtai.domain.ai.api.admin.prompt.RetireAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionListResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAiPromptVersionControllerTest {

    @Mock
    private ListAiPromptVersionsUseCase listUseCase;
    @Mock
    private GetAiPromptVersionUseCase getUseCase;
    @Mock
    private CreateAiPromptVersionUseCase createUseCase;
    @Mock
    private ActivateAiPromptVersionUseCase activateUseCase;
    @Mock
    private RetireAiPromptVersionUseCase retireUseCase;
    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        AdminAiAuthentication authentication = new AdminAiAuthentication(verifyAdminRoleUseCase);
        AdminAiPromptVersionController controller = new AdminAiPromptVersionController(
                listUseCase,
                getUseCase,
                createUseCase,
                activateUseCase,
                retireUseCase,
                authentication
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void reviewerCanManagePromptVersions() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(listUseCase.listAiPromptVersions(any())).thenReturn(new AiPromptVersionListResponse(
                List.of(promptResponse("DRAFT")), 0, 20, 1, 1, true, true, "createdAt,desc,id,desc"
        ));
        when(getUseCase.getAiPromptVersion(any())).thenReturn(promptResponse("DRAFT"));
        when(createUseCase.createAiPromptVersion(any())).thenReturn(promptResponse("DRAFT"));
        when(activateUseCase.activateAiPromptVersion(any())).thenReturn(promptResponse("ACTIVE"));
        when(retireUseCase.retireAiPromptVersion(any())).thenReturn(promptResponse("RETIRED"));

        mockMvc.perform(get("/api/v1/admin/ai/prompt-versions")
                        .principal(authentication("ROLE_ADMIN"))
                        .param("promptType", "EXPLANATION")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(2));

        mockMvc.perform(get("/api/v1/admin/ai/prompt-versions/2")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2026.06.2"));

        mockMvc.perform(post("/api/v1/admin/ai/prompt-versions")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptType":"EXPLANATION",
                                  "version":"2026.06.2",
                                  "systemPrompt":"Return JSON only.",
                                  "userPromptTemplate":"Verses: {{versesBlock}}",
                                  "modelName":"deepseek-chat",
                                  "temperature":0.2,
                                  "maxTokens":2000,
                                  "description":"candidate"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/admin/ai/prompt-versions/2/activate")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/ai/prompt-versions/2/retire")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"));
    }

    @Test
    void contentCreatorCannotListPromptVersions() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER"))))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/ai/prompt-versions")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    @Test
    void createPromptVersionInvalidTemperatureReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/prompt-versions")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptType":"EXPLANATION",
                                  "version":"2026.06.2",
                                  "systemPrompt":"Return JSON only.",
                                  "userPromptTemplate":"Verses: {{versesBlock}}",
                                  "temperature":3.0,
                                  "maxTokens":2000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingPromptVersionReturns404() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(getUseCase.getAiPromptVersion(any()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/ai/prompt-versions/999")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C0004"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static AiPromptVersionResponse promptResponse(String status) {
        return new AiPromptVersionResponse(
                2L,
                "EXPLANATION",
                "2026.06.2",
                "hash-002",
                status,
                "Return JSON only.",
                "Verses: {{versesBlock}}",
                "deepseek-chat",
                0.2,
                2000,
                "candidate",
                100L,
                OffsetDateTime.parse("2026-06-15T10:00:00+09:00"),
                "ACTIVE".equals(status) ? OffsetDateTime.parse("2026-06-15T10:10:00+09:00") : null,
                "RETIRED".equals(status) ? OffsetDateTime.parse("2026-06-15T10:20:00+09:00") : null
        );
    }
}
