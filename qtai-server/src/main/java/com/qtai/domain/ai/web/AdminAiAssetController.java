package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;

@RestController
@RequestMapping("/api/v1/admin/ai/assets")
public class AdminAiAssetController {

    private final RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    private final ListAdminAiAssetsUseCase listAdminAiAssetsUseCase;
    private final GetAdminAiAssetUseCase getAdminAiAssetUseCase;
    private final Clock clock;

    public AdminAiAssetController(
            RegenerateAiAssetUseCase regenerateAiAssetUseCase,
            ListAdminAiAssetsUseCase listAdminAiAssetsUseCase,
            GetAdminAiAssetUseCase getAdminAiAssetUseCase
    ) {
        this(regenerateAiAssetUseCase, listAdminAiAssetsUseCase, getAdminAiAssetUseCase, Clock.systemDefaultZone());
    }

    AdminAiAssetController(
            RegenerateAiAssetUseCase regenerateAiAssetUseCase,
            ListAdminAiAssetsUseCase listAdminAiAssetsUseCase,
            GetAdminAiAssetUseCase getAdminAiAssetUseCase,
            Clock clock
    ) {
        this.regenerateAiAssetUseCase = regenerateAiAssetUseCase;
        this.listAdminAiAssetsUseCase = listAdminAiAssetsUseCase;
        this.getAdminAiAssetUseCase = getAdminAiAssetUseCase;
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
        AdminAuthentication adminAuthentication = requireAdminAuthentication(authentication);
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

    @GetMapping("/{assetId}")
    public ResponseEntity<ApiResponse<AdminAiAssetDetailResponse>> getAsset(
            @PathVariable Long assetId,
            Authentication authentication
    ) {
        AdminAuthentication adminAuthentication = requireAdminAuthentication(authentication);
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
            @PathVariable Long assetId,
            Authentication authentication,
            @Valid @RequestBody RegenerateAiAssetRequest request
    ) {
        AdminAuthentication adminAuthentication = requireAdminAuthentication(authentication);
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
        HttpStatus status = switch (exception.getErrorCode()) {
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case AI_ASSET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    private static AdminAuthentication requireAdminAuthentication(Authentication requestAuthentication) {
        Authentication authentication = requestAuthentication != null
                ? requestAuthentication
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        if (!hasAuthority(authorities, "ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String adminRole = resolveAdminRole(authorities);
        if (adminRole == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return new AdminAuthentication(resolvePrincipalId(authentication), "ADMIN", adminRole);
    }

    private static Long resolvePrincipalId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        if (principal instanceof CharSequence text) {
            return parsePrincipalId(text.toString());
        }
        return parsePrincipalId(authentication.getName());
    }

    private static Long parsePrincipalId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static boolean hasAuthority(Set<String> authorities, String role) {
        return authorities.contains("ROLE_" + role);
    }

    private static String resolveAdminRole(Set<String> authorities) {
        for (String adminRole : List.of("REVIEWER", "SUPER_ADMIN")) {
            if (authorities.contains("ADMIN_ROLE_" + adminRole)) {
                return adminRole;
            }
        }
        return null;
    }

    private record AdminAuthentication(
            Long adminId,
            String memberRole,
            String adminRole
    ) {
    }
}
