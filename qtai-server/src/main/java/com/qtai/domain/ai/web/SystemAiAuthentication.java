package com.qtai.domain.ai.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

final class SystemAiAuthentication {

    private SystemAiAuthentication() {
    }

    static void requireSystemBatch(Authentication requestAuthentication) {
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
        if (!authorities.contains("SYSTEM_BATCH") && !authorities.contains("ROLE_SYSTEM_BATCH")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
