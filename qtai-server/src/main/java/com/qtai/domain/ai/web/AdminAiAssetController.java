package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetResult;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;

@RestController
@RequestMapping("/api/v1/admin/ai/assets")
public class AdminAiAssetController {

    private final RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    private final ListAdminAiAssetsUseCase listAdminAiAssetsUseCase;
    private final GetAdminAiAssetUseCase getAdminAiAssetUseCase;
    private final ReviewAiAssetUseCase reviewAiAssetUseCase;
    private final AdminAiAuthentication adminAiAuthentication;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminAiAssetController(
            RegenerateAiAssetUseCase regenerateAiAssetUseCase,
            ListAdminAiAssetsUseCase listAdminAiAssetsUseCase,
            GetAdminAiAssetUseCase getAdminAiAssetUseCase,
            ReviewAiAssetUseCase reviewAiAssetUseCase,
            AdminAiAuthentication adminAiAuthentication
    ) {
        this(
                regenerateAiAssetUseCase,
                listAdminAiAssetsUseCase,
                getAdminAiAssetUseCase,
                reviewAiAssetUseCase,
                adminAiAuthentication,
                Clock.systemDefaultZone()
        );
    }

    AdminAiAssetController(
            RegenerateAiAssetUseCase regenerateAiAssetUseCase,
            ListAdminAiAssetsUseCase listAdminAiAssetsUseCase,
            GetAdminAiAssetUseCase getAdminAiAssetUseCase,
            ReviewAiAssetUseCase reviewAiAssetUseCase,
            AdminAiAuthentication adminAiAuthentication,
            Clock clock
    ) {
        this.regenerateAiAssetUseCase = regenerateAiAssetUseCase;
        this.listAdminAiAssetsUseCase = listAdminAiAssetsUseCase;
        this.getAdminAiAssetUseCase = getAdminAiAssetUseCase;
        this.reviewAiAssetUseCase = reviewAiAssetUseCase;
        this.adminAiAuthentication = adminAiAuthentication;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminAiAssetListResponse>> listAssets(
            Authentication authentication,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long promptVersionId,
            @RequestParam(required = false) Long checklistVersionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication.AdminAiPrincipal adminAuthentication = adminAiAuthentication.requireReviewer(authentication);
        AdminAiAssetListResponse response = listAdminAiAssetsUseCase.listAdminAiAssets(new ListAdminAiAssetsQuery(
                adminAuthentication.adminId(),
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                assetType,
                targetType,
                status,
                promptVersionId,
                checklistVersionId,
                page,
                size
        ));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{assetId}/approve")
    public ResponseEntity<ApiResponse<ReviewAiAssetResult>> approve(
            @PathVariable("assetId") Long assetId,
            Authentication authentication,
            @RequestBody(required = false) AdminAiAssetReviewRequest request
    ) {
        ReviewAiAssetResult result = reviewAiAssetUseCase.reviewAiAsset(reviewCommand(
                assetId,
                authentication,
                "APPROVE",
                request,
                request != null && Boolean.TRUE.equals(request.activateForTarget())
        ));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{assetId}/reject")
    public ResponseEntity<ApiResponse<ReviewAiAssetResult>> reject(
            @PathVariable("assetId") Long assetId,
            Authentication authentication,
            @RequestBody(required = false) AdminAiAssetReviewRequest request
    ) {
        ReviewAiAssetResult result = reviewAiAssetUseCase.reviewAiAsset(reviewCommand(
                assetId,
                authentication,
                "REJECT",
                request,
                false
        ));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{assetId}/hide")
    public ResponseEntity<ApiResponse<ReviewAiAssetResult>> hide(
            @PathVariable("assetId") Long assetId,
            Authentication authentication,
            @RequestBody(required = false) AdminAiAssetReviewRequest request
    ) {
        ReviewAiAssetResult result = reviewAiAssetUseCase.reviewAiAsset(reviewCommand(
                assetId,
                authentication,
                "HIDE",
                request,
                false
        ));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<ApiResponse<AdminAiAssetDetailResponse>> getAsset(
            @PathVariable("assetId") Long assetId,
            Authentication authentication
    ) {
        AdminAiAuthentication.AdminAiPrincipal adminAuthentication = adminAiAuthentication.requireReviewer(authentication);
        AdminAiAssetDetailResponse response = getAdminAiAssetUseCase.getAdminAiAsset(new GetAdminAiAssetQuery(
                adminAuthentication.adminId(),
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                assetId
        ));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{assetId}/regenerate")
    public ResponseEntity<ApiResponse<RegenerateAiAssetResponse>> regenerate(
            @PathVariable("assetId") Long assetId,
            Authentication authentication,
            @Valid @RequestBody RegenerateAiAssetRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal adminAuthentication = adminAiAuthentication.requireReviewer(authentication);
        RegenerateAiAssetResult result = regenerateAiAssetUseCase.regenerateAiAsset(new RegenerateAiAssetCommand(
                adminAuthentication.adminId(),
                assetId,
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                request.reason(),
                request.promptVersionId(),
                OffsetDateTime.now(clock)
        ));

        return ResponseEntity.accepted().body(ApiResponse.success(new RegenerateAiAssetResponse(
                result.generationJobId(),
                result.status(),
                result.createdAt()
        )));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        // 상태 매핑은 ai web 공통 매핑(AiWebExceptionResponses)에 위임 — 컨트롤러별 중복 switch 제거
        return AiWebExceptionResponses.business(exception);
    }

    private ReviewAiAssetCommand reviewCommand(
            Long assetId,
            Authentication authentication,
            String action,
            AdminAiAssetReviewRequest request,
            boolean activateForTarget
    ) {
        AdminAiAuthentication.AdminAiPrincipal adminAuthentication = adminAiAuthentication.requireReviewer(authentication);
        return new ReviewAiAssetCommand(
                adminAuthentication.adminId(),
                assetId,
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                action,
                request == null ? null : request.reason(),
                activateForTarget,
                OffsetDateTime.now(clock)
        );
    }

    record AdminAiAssetReviewRequest(
            String reason,
            Boolean activateForTarget
    ) {
    }
}
