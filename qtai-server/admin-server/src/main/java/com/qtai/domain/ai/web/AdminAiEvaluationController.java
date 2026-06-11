package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
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
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseStatusResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationAssetCandidateCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationCaseCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationSetCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationCaseQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationSetQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationCasesQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationSetsQuery;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiEvaluationController {

    private final ListAiEvaluationSetsUseCase listSetsUseCase;
    private final CreateAiEvaluationSetUseCase createSetUseCase;
    private final GetAiEvaluationSetUseCase getSetUseCase;
    private final ActivateAiEvaluationSetUseCase activateSetUseCase;
    private final RetireAiEvaluationSetUseCase retireSetUseCase;
    private final ListAiEvaluationCasesUseCase listCasesUseCase;
    private final CreateAiEvaluationCaseUseCase createCaseUseCase;
    private final GetAiEvaluationCaseUseCase getCaseUseCase;
    private final ApproveAiEvaluationCaseUseCase approveCaseUseCase;
    private final RejectAiEvaluationCaseUseCase rejectCaseUseCase;
    private final CreateAiEvaluationAssetCandidateUseCase assetCandidateUseCase;
    private final AdminAiAuthentication adminAiAuthentication;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AdminAiEvaluationController(
            ListAiEvaluationSetsUseCase listSetsUseCase,
            CreateAiEvaluationSetUseCase createSetUseCase,
            GetAiEvaluationSetUseCase getSetUseCase,
            ActivateAiEvaluationSetUseCase activateSetUseCase,
            RetireAiEvaluationSetUseCase retireSetUseCase,
            ListAiEvaluationCasesUseCase listCasesUseCase,
            CreateAiEvaluationCaseUseCase createCaseUseCase,
            GetAiEvaluationCaseUseCase getCaseUseCase,
            ApproveAiEvaluationCaseUseCase approveCaseUseCase,
            RejectAiEvaluationCaseUseCase rejectCaseUseCase,
            CreateAiEvaluationAssetCandidateUseCase assetCandidateUseCase,
            AdminAiAuthentication adminAiAuthentication,
            ObjectMapper objectMapper
    ) {
        this(listSetsUseCase, createSetUseCase, getSetUseCase, activateSetUseCase, retireSetUseCase,
                listCasesUseCase, createCaseUseCase, getCaseUseCase, approveCaseUseCase, rejectCaseUseCase,
                assetCandidateUseCase, adminAiAuthentication, objectMapper, Clock.systemDefaultZone());
    }

    AdminAiEvaluationController(
            ListAiEvaluationSetsUseCase listSetsUseCase,
            CreateAiEvaluationSetUseCase createSetUseCase,
            GetAiEvaluationSetUseCase getSetUseCase,
            ActivateAiEvaluationSetUseCase activateSetUseCase,
            RetireAiEvaluationSetUseCase retireSetUseCase,
            ListAiEvaluationCasesUseCase listCasesUseCase,
            CreateAiEvaluationCaseUseCase createCaseUseCase,
            GetAiEvaluationCaseUseCase getCaseUseCase,
            ApproveAiEvaluationCaseUseCase approveCaseUseCase,
            RejectAiEvaluationCaseUseCase rejectCaseUseCase,
            CreateAiEvaluationAssetCandidateUseCase assetCandidateUseCase,
            AdminAiAuthentication adminAiAuthentication
    ) {
        this(listSetsUseCase, createSetUseCase, getSetUseCase, activateSetUseCase, retireSetUseCase,
                listCasesUseCase, createCaseUseCase, getCaseUseCase, approveCaseUseCase, rejectCaseUseCase,
                assetCandidateUseCase, adminAiAuthentication, new ObjectMapper().findAndRegisterModules(),
                Clock.systemDefaultZone());
    }

    AdminAiEvaluationController(
            ListAiEvaluationSetsUseCase listSetsUseCase,
            CreateAiEvaluationSetUseCase createSetUseCase,
            GetAiEvaluationSetUseCase getSetUseCase,
            ActivateAiEvaluationSetUseCase activateSetUseCase,
            RetireAiEvaluationSetUseCase retireSetUseCase,
            ListAiEvaluationCasesUseCase listCasesUseCase,
            CreateAiEvaluationCaseUseCase createCaseUseCase,
            GetAiEvaluationCaseUseCase getCaseUseCase,
            ApproveAiEvaluationCaseUseCase approveCaseUseCase,
            RejectAiEvaluationCaseUseCase rejectCaseUseCase,
            CreateAiEvaluationAssetCandidateUseCase assetCandidateUseCase,
            AdminAiAuthentication adminAiAuthentication,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.listSetsUseCase = listSetsUseCase;
        this.createSetUseCase = createSetUseCase;
        this.getSetUseCase = getSetUseCase;
        this.activateSetUseCase = activateSetUseCase;
        this.retireSetUseCase = retireSetUseCase;
        this.listCasesUseCase = listCasesUseCase;
        this.createCaseUseCase = createCaseUseCase;
        this.getCaseUseCase = getCaseUseCase;
        this.approveCaseUseCase = approveCaseUseCase;
        this.rejectCaseUseCase = rejectCaseUseCase;
        this.assetCandidateUseCase = assetCandidateUseCase;
        this.adminAiAuthentication = adminAiAuthentication;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @GetMapping("/evaluation-sets")
    public ResponseEntity<ApiResponse<AiEvaluationSetListResponse>> listSets(
            Authentication authentication,
            @RequestParam(required = false) String evalType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(listSetsUseCase.listEvaluationSets(new ListAiEvaluationSetsQuery(
                principal.adminId(), principal.memberRole(), principal.adminRole(),
                evalType, targetType, status, page, size
        ))));
    }

    @PostMapping("/evaluation-sets")
    public ResponseEntity<ApiResponse<AiEvaluationSetResponse>> createSet(
            Authentication authentication,
            @Valid @RequestBody AiEvaluationSetRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        AiEvaluationSetResponse response = createSetUseCase.createEvaluationSet(new CreateAiEvaluationSetCommand(
                principal.adminId(), principal.memberRole(), principal.adminRole(),
                request.name(), request.evalType(), request.version(), request.targetType(),
                json(request.expectedPolicyJson()), request.description(), request.status()
        ));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/evaluation-sets/{setId}")
    public ResponseEntity<ApiResponse<AiEvaluationSetResponse>> getSet(Authentication authentication, @PathVariable Long setId) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(getSetUseCase.getEvaluationSet(new GetAiEvaluationSetQuery(
                principal.adminId(), principal.memberRole(), principal.adminRole(), setId
        ))));
    }

    @PostMapping("/evaluation-sets/{setId}/activate")
    public ResponseEntity<ApiResponse<AiEvaluationSetResponse>> activateSet(Authentication authentication, @PathVariable Long setId) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(activateSetUseCase.activateEvaluationSet(
                new ChangeAiEvaluationSetStatusCommand(principal.adminId(), principal.memberRole(), principal.adminRole(), setId)
        )));
    }

    @PostMapping("/evaluation-sets/{setId}/retire")
    public ResponseEntity<ApiResponse<AiEvaluationSetResponse>> retireSet(Authentication authentication, @PathVariable Long setId) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(retireSetUseCase.retireEvaluationSet(
                new ChangeAiEvaluationSetStatusCommand(principal.adminId(), principal.memberRole(), principal.adminRole(), setId)
        )));
    }

    @GetMapping("/evaluation-sets/{setId}/cases")
    public ResponseEntity<ApiResponse<AiEvaluationCaseListResponse>> listCases(
            Authentication authentication,
            @PathVariable Long setId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(listCasesUseCase.listEvaluationCases(new ListAiEvaluationCasesQuery(
                principal.adminId(), principal.memberRole(), principal.adminRole(), setId, status, page, size
        ))));
    }

    @PostMapping("/evaluation-sets/{setId}/cases")
    public ResponseEntity<ApiResponse<AiEvaluationCaseResponse>> createCase(
            Authentication authentication,
            @PathVariable Long setId,
            @Valid @RequestBody AiEvaluationCaseRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        AiEvaluationCaseResponse response = createCaseUseCase.createEvaluationCase(new CreateAiEvaluationCaseCommand(
                principal.adminId(), principal.memberRole(), principal.adminRole(), setId,
                request.targetType(), request.targetId(), request.sourceType(), request.sourceId(),
                json(request.inputJson()), json(request.expectedOutputJson()), json(request.expectedPolicyJson()),
                request.status()
        ));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/evaluation-cases/{caseId}")
    public ResponseEntity<ApiResponse<AiEvaluationCaseResponse>> getCase(Authentication authentication, @PathVariable Long caseId) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(getCaseUseCase.getEvaluationCase(new GetAiEvaluationCaseQuery(
                principal.adminId(), principal.memberRole(), principal.adminRole(), caseId
        ))));
    }

    @PostMapping("/evaluation-cases/{caseId}/approve")
    public ResponseEntity<ApiResponse<AiEvaluationCaseStatusResponse>> approveCase(
            Authentication authentication,
            @PathVariable Long caseId,
            @Valid @RequestBody ReviewRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(approveCaseUseCase.approveEvaluationCase(reviewCommand(principal, caseId, request))));
    }

    @PostMapping("/evaluation-cases/{caseId}/reject")
    public ResponseEntity<ApiResponse<AiEvaluationCaseStatusResponse>> rejectCase(
            Authentication authentication,
            @PathVariable Long caseId,
            @Valid @RequestBody ReviewRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(rejectCaseUseCase.rejectEvaluationCase(reviewCommand(principal, caseId, request))));
    }

    @PostMapping("/assets/{assetId}/evaluation-candidates")
    public ResponseEntity<ApiResponse<AiEvaluationCaseResponse>> createAssetCandidate(
            Authentication authentication,
            @PathVariable Long assetId,
            @Valid @RequestBody AssetCandidateRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireEvaluationManager(authentication);
        AiEvaluationCaseResponse response = assetCandidateUseCase.createAssetCandidate(new CreateAiEvaluationAssetCandidateCommand(
                principal.adminId(), principal.memberRole(), principal.adminRole(),
                request.evaluationSetId(), assetId, json(request.expectedPolicyJson())
        ));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        return AiWebExceptionResponses.invalidInput();
    }

    private ChangeAiEvaluationCaseStatusCommand reviewCommand(
            AdminAiAuthentication.AdminAiPrincipal principal,
            Long caseId,
            ReviewRequest request
    ) {
        return new ChangeAiEvaluationCaseStatusCommand(
                principal.adminId(),
                principal.memberRole(),
                principal.adminRole(),
                caseId,
                request.reviewReason(),
                OffsetDateTime.now(clock)
        );
    }

    private String json(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "invalid JSON request");
        }
    }

    public record AiEvaluationSetRequest(
            @NotBlank String name,
            @NotBlank String evalType,
            @NotBlank String version,
            @NotBlank String targetType,
            JsonNode expectedPolicyJson,
            String description,
            String status
    ) {
    }

    public record AiEvaluationCaseRequest(
            @NotBlank String targetType,
            Long targetId,
            @NotBlank String sourceType,
            Long sourceId,
            @NotNull JsonNode inputJson,
            JsonNode expectedOutputJson,
            JsonNode expectedPolicyJson,
            String status
    ) {
    }

    public record ReviewRequest(@NotBlank String reviewReason) {
    }

    public record AssetCandidateRequest(
            @NotNull Long evaluationSetId,
            JsonNode expectedPolicyJson
    ) {
    }
}
