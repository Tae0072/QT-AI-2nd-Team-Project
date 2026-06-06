package com.qtai.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;

/**
 * 테스트용 {@link VerifyAdminRoleUseCase} 스텁.
 *
 * <p>web 단위 테스트에서 admin_users DB 검증을 흉내 낸다.
 * {@link #register(Long, String)}로 memberId→admin_role을 등록하면
 * 실제 구현(AdminService)과 동일한 규칙으로 검증한다:
 * <ul>
 *   <li>미등록 memberId → ADMIN_USER_NOT_FOUND</li>
 *   <li>{@link #disable(Long)}된 계정 → ADMIN_USER_DISABLED</li>
 *   <li>SUPER_ADMIN은 모든 요구 역할 통과(우월권)</li>
 *   <li>요구 역할 불일치 → ADMIN_ROLE_INSUFFICIENT</li>
 * </ul>
 *
 * <p>반환되는 adminUserId는 {@code memberId + ADMIN_USER_ID_OFFSET}로 고정해
 * "쿼리/커맨드에 admin_users.id가 들어가는지"(memberId 오기록 회귀)를 검증할 수 있게 한다.
 */
public class StubVerifyAdminRoleUseCase implements VerifyAdminRoleUseCase {

    /** adminUserId = memberId + OFFSET 규약 — memberId 오기록 회귀 검출용. */
    public static final long ADMIN_USER_ID_OFFSET = 100L;

    private final Map<Long, String> adminRolesByMemberId = new HashMap<>();
    private final Set<Long> disabledMemberIds = new HashSet<>();

    /** memberId에 admin_role을 등록한다 (admin_users 행 시뮬레이션). */
    public StubVerifyAdminRoleUseCase register(Long memberId, String adminRole) {
        adminRolesByMemberId.put(memberId, adminRole);
        return this;
    }

    /** 해당 memberId 계정을 DISABLED 상태로 만든다. */
    public StubVerifyAdminRoleUseCase disable(Long memberId) {
        disabledMemberIds.add(memberId);
        return this;
    }

    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        return toInfo(memberId, findActiveRole(memberId));
    }

    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        return verifyAnyRole(memberId, List.of(requiredRole));
    }

    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, Collection<String> requiredRoles) {
        String adminRole = findActiveRole(memberId);
        if ("SUPER_ADMIN".equals(adminRole)
                || (requiredRoles != null && requiredRoles.contains(adminRole))) {
            return toInfo(memberId, adminRole);
        }
        throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }

    private String findActiveRole(Long memberId) {
        if (!adminRolesByMemberId.containsKey(memberId)) {
            throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
        }
        if (disabledMemberIds.contains(memberId)) {
            throw new BusinessException(ErrorCode.ADMIN_USER_DISABLED);
        }
        return adminRolesByMemberId.get(memberId);
    }

    private static AdminUserInfo toInfo(Long memberId, String adminRole) {
        return new AdminUserInfo(memberId + ADMIN_USER_ID_OFFSET, memberId, adminRole);
    }
}
