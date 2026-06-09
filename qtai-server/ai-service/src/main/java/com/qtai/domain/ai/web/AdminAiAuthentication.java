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
import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminAuthResult;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminRole;

@Component
public class AdminAiAuthentication {

    private final AdminAuthClient adminAuthClient;

    AdminAiAuthentication(AdminAuthClient adminAuthClient) {
        this.adminAuthClient = adminAuthClient;
    }

    AdminAiPrincipal requireReviewer(Authentication requestAuthentication) {
        return require(requestAuthentication, List.of(AdminRole.REVIEWER));
    }

    AdminAiPrincipal requireMonitoring(Authentication requestAuthentication) {
        return require(requestAuthentication, List.of(AdminRole.OPERATOR, AdminRole.REVIEWER));
    }

    private AdminAiPrincipal require(Authentication requestAuthentication, List<AdminRole> requiredRoles) {
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
        AdminAuthResult adminUserInfo = adminAuthClient.verifyAnyRole(memberId, requiredRoles);
        return new AdminAiPrincipal(
                adminUserInfo.adminUserId(),
                adminUserInfo.memberId(),
                "ADMIN",
                adminUserInfo.adminRole().name()
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
