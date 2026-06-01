package com.qtai.domain.ai.web;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

record AdminAiAuthentication(
        Long adminId,
        String memberRole,
        String adminRole
) {

    static AdminAiAuthentication requireReviewer(Authentication requestAuthentication) {
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

        String adminRole = resolveReviewerRole(authorities);
        if (adminRole == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return new AdminAiAuthentication(resolvePrincipalId(authentication), "ADMIN", adminRole);
    }

    private static String resolveReviewerRole(Set<String> authorities) {
        for (String adminRole : List.of("REVIEWER", "SUPER_ADMIN")) {
            if (authorities.contains("ADMIN_ROLE_" + adminRole)) {
                return adminRole;
            }
        }
        return null;
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
}
