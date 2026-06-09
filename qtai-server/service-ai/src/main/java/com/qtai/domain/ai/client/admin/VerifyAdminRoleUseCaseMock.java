package com.qtai.domain.ai.client.admin;

import java.util.Collection;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import org.springframework.stereotype.Component;

/**
 * admin 도메인 {@link VerifyAdminRoleUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>관리자 권한 이중검증(members.role=ADMIN + admin_users.admin_role)은 admin-server 소관이며,
 * service-ai의 관리자 경로(/api/v1/admin/**)는 {@link com.qtai.ai.SecurityConfig}에서 denyAll로
 * 차단한다. 따라서 이 Mock은 실제로 호출되지 않아야 한다.
 *
 * <p>혹시라도 호출되면(예: 향후 denyAll 정책이 완화되는 회귀) 가짜 권한을 부여하지 않고
 * {@link ErrorCode#ADMIN_ROLE_INSUFFICIENT}(403)로 거부한다. {@code RuntimeException}이 아니라
 * {@link BusinessException}으로 던져, 공통 예외 핸들러가 500이 아닌 403으로 매핑하도록 한다
 * (claude-review 지적 반영: 인가 경로에서 500 누출 방지). 통합 시 RestClient 어댑터로 교체한다.
 */
@Component("aiVerifyAdminRoleUseCaseMock")
public class VerifyAdminRoleUseCaseMock implements VerifyAdminRoleUseCase {

    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }

    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }

    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, Collection<String> requiredRoles) {
        throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }
}
