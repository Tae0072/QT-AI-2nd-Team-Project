package com.qtai.domain.ai.client.admin;

import java.util.Collection;

import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import org.springframework.stereotype.Component;

/**
 * admin 도메인 {@link VerifyAdminRoleUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>관리자 권한 이중검증(members.role=ADMIN + admin_users.admin_role)은 admin-server 소관이며,
 * service-ai의 관리자 경로(/api/v1/admin/**)는 {@link com.qtai.ai.SecurityConfig}에서 denyAll로
 * 차단한다. 따라서 이 Mock은 실제로 호출되지 않아야 한다. 혹시라도 호출되면 가짜 권한을 부여하지
 * 않고 즉시 실패시켜(가짜 인가 방지) 잘못된 경로를 드러낸다. 통합 시 RestClient 어댑터로 교체한다.
 */
@Component("aiVerifyAdminRoleUseCaseMock")
public class VerifyAdminRoleUseCaseMock implements VerifyAdminRoleUseCase {

    private static final String NOT_SUPPORTED =
            "관리자 권한 검증은 admin-server 소관입니다. service-ai에서는 /api/v1/admin/** 을 차단하며, "
                    + "통합 단계에서 RestClient 어댑터로 교체합니다.";

    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, Collection<String> requiredRoles) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }
}
