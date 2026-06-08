package com.qtai.domain.audit.web;

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
 * audit 도메인 admin API 공통 인가 헬퍼.
 *
 * <p>CLAUDE.md §5: 관리자 API는 members.role=ADMIN(JWT)과
 * admin_users.admin_role(DB)을 모두 확인한다. 감사 로그 조회는
 * OPERATOR/REVIEWER(+SUPER_ADMIN 우월권)만 허용한다.
 *
 * <p>기존 구현은 JWT authority의 {@code ADMIN_ROLE_*}를 파싱했으나,
 * 토큰 발급 경로가 해당 authority를 만들지 않아 운영에서 전부 403이 나는
 * 결함이 있어 DB 검증 방식으로 통일했다(2026-06-05 Lead 결정).
 */
@Component
class AdminAuditAuthentication {

    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    AdminAuditAuthentication(VerifyAdminRoleUseCase verifyAdminRoleUseCase) {
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
    }

    /** 감사 로그 조회 권한 — OPERATOR/REVIEWER (SUPER_ADMIN 자동 포함). */
    AdminAuditPrincipal requireAudit(Authentication requestAuthentication) {
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
        AdminUserInfo adminUserInfo = verifyAdminRoleUseCase.verifyAnyRole(
                memberId, List.of("OPERATOR", "REVIEWER"));
        return new AdminAuditPrincipal(
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

    /**
     * 인가 결과 주체 정보.
     *
     * @param adminId    admin_users.id — 감사 조회 주체 기록용
     * @param memberId   members.id (JWT principal)
     * @param memberRole 항상 "ADMIN" (1차 검증 통과 표시)
     * @param adminRole  DB에서 확인된 세부 역할
     */
    record AdminAuditPrincipal(
            Long adminId,
            Long memberId,
            String memberRole,
            String adminRole
    ) {
    }
}
