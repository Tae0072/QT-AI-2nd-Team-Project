package com.qtai.domain.admin.api;

import com.qtai.domain.admin.api.dto.AdminUserInfo;

/**
 * 관리자 권한 검증 UseCase 포트.
 *
 * <p>CLAUDE.md §5: 관리자 API는 members.role=ADMIN과 admin_users.admin_role을
 * 모두 확인한 뒤, OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN 중
 * API 명세에 맞는 세부 권한을 요구한다.
 *
 * <p>Spring Security가 ROLE_ADMIN을 1차 검증한 뒤,
 * 이 UseCase로 admin_users 테이블의 세부 권한을 2차 검증한다.
 *
 * <p>다른 도메인(ai, audit 등)에서 관리자 권한이 필요할 때
 * 이 UseCase를 통해 검증한다 (도메인 간 직접 Entity 참조 금지).
 */
public interface VerifyAdminRoleUseCase {

    /**
     * memberId에 해당하는 활성 관리자 정보를 조회한다.
     *
     * @param memberId JWT에서 추출한 회원 ID
     * @return 관리자 정보 (adminUserId, adminRole)
     * @throws com.qtai.common.exception.BusinessException ADMIN_USER_NOT_FOUND, ADMIN_USER_DISABLED
     */
    AdminUserInfo getActiveAdmin(Long memberId);

    /**
     * 특정 관리자 역할이 필요한 작업에서 권한을 검증한다.
     *
     * <p>SUPER_ADMIN은 모든 역할을 포함한다.
     *
     * @param memberId     JWT에서 추출한 회원 ID
     * @param requiredRole 필요한 관리자 역할 문자열 (예: "OPERATOR", "REVIEWER")
     * @return 관리자 정보 (adminUserId, adminRole)
     * @throws com.qtai.common.exception.BusinessException ADMIN_USER_NOT_FOUND, ADMIN_USER_DISABLED, ADMIN_ROLE_INSUFFICIENT
     */
    AdminUserInfo verifyRole(Long memberId, String requiredRole);

    /**
     * 허용 역할 목록 중 하나라도 충족하면 통과하는 권한 검증.
     *
     * <p>SUPER_ADMIN은 모든 역할을 포함하므로 목록에 없어도 항상 통과한다.
     * (예: requiredRoles=["OPERATOR","REVIEWER"]면 OPERATOR/REVIEWER/SUPER_ADMIN 허용)
     *
     * @param memberId      JWT에서 추출한 회원 ID
     * @param requiredRoles 허용 관리자 역할 문자열 목록 (비어 있으면 ADMIN_ROLE_INSUFFICIENT)
     * @return 관리자 정보 (adminUserId, adminRole)
     * @throws com.qtai.common.exception.BusinessException ADMIN_USER_NOT_FOUND, ADMIN_USER_DISABLED, ADMIN_ROLE_INSUFFICIENT
     */
    AdminUserInfo verifyAnyRole(Long memberId, java.util.Collection<String> requiredRoles);
}
