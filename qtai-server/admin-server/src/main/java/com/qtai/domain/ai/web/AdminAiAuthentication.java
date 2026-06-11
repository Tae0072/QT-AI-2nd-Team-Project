package com.qtai.domain.ai.web;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;

/**
 * Common authentication helper for admin AI APIs.
 *
 * <p>Admin APIs require both ROLE_ADMIN from the access token and an active
 * admin_users.admin_role verified through {@link VerifyAdminRoleUseCase}.
 */
@Component
class AdminAiAuthentication {

    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    AdminAiAuthentication(VerifyAdminRoleUseCase verifyAdminRoleUseCase) {
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
    }

    /** AI asset review access. REVIEWER, with SUPER_ADMIN override. */
    AdminAiPrincipal requireReviewer(Authentication requestAuthentication) {
        return require(requestAuthentication, List.of("REVIEWER"));
    }

    /** AI evaluation set/case management. REVIEWER or CONTENT_CREATOR, with SUPER_ADMIN override. */
    AdminAiPrincipal requireEvaluationManager(Authentication requestAuthentication) {
        return require(requestAuthentication, List.of("REVIEWER", "CONTENT_CREATOR"));
    }

    /** AI monitoring read access. OPERATOR or REVIEWER, with SUPER_ADMIN override. */
    AdminAiPrincipal requireMonitoring(Authentication requestAuthentication) {
        return require(requestAuthentication, List.of("OPERATOR", "REVIEWER"));
    }

    private AdminAiPrincipal require(Authentication requestAuthentication, List<String> requiredRoles) {
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
        AdminUserInfo adminUserInfo = verifyAdminRoleUseCase.verifyAnyRole(memberId, requiredRoles);
        return new AdminAiPrincipal(
                adminUserInfo.adminUserId(),
                adminUserInfo.memberId(),
                "ADMIN",
                adminUserInfo.adminRole()
        );
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

    record AdminAiPrincipal(
            Long adminId,
            Long memberId,
            String memberRole,
            String adminRole
    ) {
    }
}
