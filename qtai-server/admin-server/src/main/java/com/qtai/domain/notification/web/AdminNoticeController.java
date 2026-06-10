package com.qtai.domain.notification.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.notification.api.CreateAdminNoticeUseCase;
import com.qtai.domain.notification.api.HideAdminNoticeUseCase;
import com.qtai.domain.notification.api.ListAdminNoticesUseCase;
import com.qtai.domain.notification.api.PublishAdminNoticeUseCase;
import com.qtai.domain.notification.api.UpdateAdminNoticeUseCase;
import com.qtai.domain.notification.api.dto.AdminNoticeCommand;
import com.qtai.domain.notification.api.dto.AdminNoticeDetailResponse;
import com.qtai.domain.notification.api.dto.AdminNoticeListResponse;
import com.qtai.domain.notification.api.dto.AdminNoticePublishResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final ListAdminNoticesUseCase listAdminNoticesUseCase;
    private final CreateAdminNoticeUseCase createAdminNoticeUseCase;
    private final UpdateAdminNoticeUseCase updateAdminNoticeUseCase;
    private final PublishAdminNoticeUseCase publishAdminNoticeUseCase;
    private final HideAdminNoticeUseCase hideAdminNoticeUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    public record NoticeRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 10_000) String body,
            String status
    ) {
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminNoticeListResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(listAdminNoticesUseCase.listAdminNotices(page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> create(
            Authentication authentication,
            @Valid @RequestBody NoticeRequest request) {
        Long adminUserId = requireOperator(authentication);
        AdminNoticeDetailResponse response = createAdminNoticeUseCase.createNotice(toCommand(adminUserId, request));
        return ResponseEntity.created(URI.create("/api/v1/admin/notices/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody NoticeRequest request) {
        Long adminUserId = requireOperator(authentication);
        AdminNoticeDetailResponse response = updateAdminNoticeUseCase.updateNotice(id, toCommand(adminUserId, request));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AdminNoticePublishResponse>> publish(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminUserId = requireOperator(authentication);
        AdminNoticePublishResponse response = publishAdminNoticeUseCase.publishNotice(adminUserId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<Void> hide(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminUserId = requireOperator(authentication);
        hideAdminNoticeUseCase.hideNotice(adminUserId, id);
        return ResponseEntity.noContent().build();
    }

    private static AdminNoticeCommand toCommand(Long adminUserId, NoticeRequest request) {
        return new AdminNoticeCommand(adminUserId, request.title(), request.body(), request.status());
    }

    private Long requireOperator(Authentication requestAuthentication) {
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
        if (!authorities.contains("ROLE_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long memberId = resolvePrincipalId(authentication);
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, List.of("OPERATOR")).adminUserId();
    }

    private static Long resolvePrincipalId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(principal));
        } catch (NumberFormatException e) {
            try {
                return Long.valueOf(authentication.getName());
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
    }
}
