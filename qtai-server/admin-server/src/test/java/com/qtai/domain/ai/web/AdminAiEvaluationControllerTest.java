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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.evaluation.ActivateAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ApproveAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationAssetCandidateUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ListAiEvaluationCasesUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ListAiEvaluationSetsUseCase;
import com.qtai.domain.ai.api.admin.evaluation.RejectAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.RetireAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseStatusResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAiEvaluationControllerTest {

    @Mock
    private ListAiEvaluationSetsUseCase listSetsUseCase;
    @Mock
    private CreateAiEvaluationSetUseCase createSetUseCase;
    @Mock
    private GetAiEvaluationSetUseCase getSetUseCase;
    @Mock
    private ActivateAiEvaluationSetUseCase activateSetUseCase;
    @Mock
    private RetireAiEvaluationSetUseCase retireSetUseCase;
    @Mock
    private ListAiEvaluationCasesUseCase listCasesUseCase;
    @Mock
    private CreateAiEvaluationCaseUseCase createCaseUseCase;
    @Mock
    private GetAiEvaluationCaseUseCase getCaseUseCase;
    @Mock
    private ApproveAiEvaluationCaseUseCase approveCaseUseCase;
    @Mock
    private RejectAiEvaluationCaseUseCase rejectCaseUseCase;
    @Mock
    private CreateAiEvaluationAssetCandidateUseCase assetCandidateUseCase;
    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AdminAiAuthentication authentication = new AdminAiAuthentication(verifyAdminRoleUseCase);
        AdminAiEvaluationController controller = new AdminAiEvaluationController(
                listSetsUseCase,
                createSetUseCase,
                getSetUseCase,
                activateSetUseCase,
                retireSetUseCase,
                listCasesUseCase,
                createCaseUseCase,
                getCaseUseCase,
                approveCaseUseCase,
                rejectCaseUseCase,
                assetCandidateUseCase,
                authentication
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void contentCreatorCanCreateAndListEvaluationSets() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER", "CONTENT_CREATOR"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "CONTENT_CREATOR"));
        when(listSetsUseCase.listEvaluationSets(any())).thenReturn(new AiEvaluationSetListResponse(
                List.of(setResponse("DRAFT")), 0, 20, 1, 1, true, true, "createdAt,desc,id,desc"
        ));
        when(createSetUseCase.createEvaluationSet(any())).thenReturn(setResponse("DRAFT"));

        mockMvc.perform(get("/api/v1/admin/ai/evaluation-sets")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(20));

        mockMvc.perform(post("/api/v1/admin/ai/evaluation-sets")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"AI Q&A 정책 평가",
                                  "evalType":"QA",
                                  "version":"2026.06.1",
                                  "targetType":"QA_REQUEST",
                                  "expectedPolicyJson":{"blocked":"VALUE_JUDGMENT"},
                                  "description":"평가 셋"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void reviewerCanApproveAndRejectEvaluationCases() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(approveCaseUseCase.approveEvaluationCase(any()))
                .thenReturn(new AiEvaluationCaseStatusResponse(301L, "APPROVED"));
        when(rejectCaseUseCase.rejectEvaluationCase(any()))
                .thenReturn(new AiEvaluationCaseStatusResponse(302L, "REJECTED"));

        mockMvc.perform(post("/api/v1/admin/ai/evaluation-cases/301/approve")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewReason\":\"평가에 적합\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/v1/admin/ai/evaluation-cases/302/reject")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewReason\":\"중복\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void contentCreatorCannotApproveEvaluationCase() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER"))))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(post("/api/v1/admin/ai/evaluation-cases/301/approve")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewReason\":\"not allowed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    @Test
    void userRoleCannotListEvaluationSets() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/evaluation-sets")
                        .principal(authentication("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    @Test
    void assetCandidateEndpointReturnsCreated() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("REVIEWER", "CONTENT_CREATOR"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(assetCandidateUseCase.createAssetCandidate(any())).thenReturn(caseResponse("CANDIDATE"));

        mockMvc.perform(post("/api/v1/admin/ai/assets/500/evaluation-candidates")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "evaluationSetId":20,
                                  "expectedPolicyJson":{"expectedResult":"REJECTED"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(301))
                .andExpect(jsonPath("$.data.status").value("CANDIDATE"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static AiEvaluationSetResponse setResponse(String status) {
        return new AiEvaluationSetResponse(
                20L,
                "AI Q&A 정책 평가",
                "QA",
                "2026.06.1",
                "QA_REQUEST",
                "{\"blocked\":\"VALUE_JUDGMENT\"}",
                "평가 셋",
                status,
                OffsetDateTime.parse("2026-06-11T10:00:00+09:00"),
                null,
                null
        );
    }

    private static AiEvaluationCaseResponse caseResponse(String status) {
        return new AiEvaluationCaseResponse(
                301L,
                20L,
                "QA_REQUEST",
                1001L,
                "ADMIN_CREATED",
                null,
                "{\"question\":\"test\"}",
                null,
                "{\"expectedResult\":\"REJECTED\"}",
                status,
                null,
                null,
                OffsetDateTime.parse("2026-06-11T10:00:00+09:00")
        );
    }
}
