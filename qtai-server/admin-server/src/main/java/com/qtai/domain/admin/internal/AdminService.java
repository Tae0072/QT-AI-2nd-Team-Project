package com.qtai.domain.admin.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 도메인 진입점.
 *
 * <p>W3 1단계: 관리자 권한 검증 로직 구현.
 * <p>CLAUDE.md §5: 관리자 API는 members.role=ADMIN과 admin_users.admin_role을
 * 모두 확인한 뒤, OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN 중
 * API 명세에 맞는 세부 권한을 요구한다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService implements VerifyAdminRoleUseCase {

    private final AdminUserRepository adminUserRepository;

    /**
     * memberId에 해당하는 활성 관리자 정보를 조회한다.
     *
     * <p>Spring Security가 ROLE_ADMIN을 1차 검증한 상태에서,
     * admin_users 테이블에서 세부 정보를 2차 조회한다.
     *
     * @param memberId JWT에서 추출한 회원 ID
     * @return 관리자 정보
     * @throws BusinessException ADMIN_USER_NOT_FOUND — admin_users에 레코드 없음
     * @throws BusinessException ADMIN_USER_DISABLED — 관리자 계정 비활성
     */
    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        AdminUser adminUser = findActiveAdminUser(memberId);
        return toAdminUserInfo(adminUser);
    }

    /**
     * 특정 관리자 역할이 필요한 작업에서 권한을 검증한다.
     *
     * <p>SUPER_ADMIN은 모든 역할을 포함한다.
     *
     * @param memberId     JWT에서 추출한 회원 ID
     * @param requiredRole 필요한 역할 문자열 (예: "OPERATOR", "REVIEWER")
     * @return 관리자 정보
     * @throws BusinessException ADMIN_USER_NOT_FOUND, ADMIN_USER_DISABLED, ADMIN_ROLE_INSUFFICIENT
     */
    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        // 1) 활성 관리자 조회 (DB 1회만 조회)
        AdminUser adminUser = findActiveAdminUser(memberId);

        // 2) 역할 문자열 → enum 변환 (잘못된 문자열은 BusinessException으로 처리)
        AdminRole required;
        try {
            required = AdminRole.valueOf(requiredRole);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 관리자 역할 문자열 — memberId={}, requiredRole={}",
                    memberId, requiredRole);
            throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
        }

        // 3) 세부 역할 검증 — SUPER_ADMIN은 모든 역할을 포함
        if (!adminUser.hasRole(required)) {
            log.warn("관리자 권한 부족 — memberId={}, adminRole={}, requiredRole={}",
                    memberId, adminUser.getAdminRole(), requiredRole);
            throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
        }

        return toAdminUserInfo(adminUser);
    }

    /**
     * 허용 역할 목록 중 하나라도 충족하면 통과하는 권한 검증.
     *
     * <p>SUPER_ADMIN은 {@link AdminUser#hasRole}의 우월권 규칙에 따라
     * 목록에 명시되지 않아도 항상 통과한다.
     *
     * @param memberId      JWT에서 추출한 회원 ID
     * @param requiredRoles 허용 역할 문자열 목록 (예: ["OPERATOR", "REVIEWER"])
     * @return 관리자 정보
     * @throws BusinessException ADMIN_USER_NOT_FOUND, ADMIN_USER_DISABLED, ADMIN_ROLE_INSUFFICIENT
     */
    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, java.util.Collection<String> requiredRoles) {
        AdminUser adminUser = findActiveAdminUser(memberId);

        // SUPER_ADMIN 우월권: requiredRoles가 비어 있거나 모두 무효한 문자열이어도 항상 통과한다
        // (VerifyAdminRoleUseCase 계약). 빈/무효 목록에서 SUPER_ADMIN이 차단되던 결함 수정.
        if (adminUser.getAdminRole() == AdminRole.SUPER_ADMIN) {
            return toAdminUserInfo(adminUser);
        }

        if (requiredRoles != null) {
            for (String roleName : requiredRoles) {
                AdminRole required;
                try {
                    required = AdminRole.valueOf(roleName);
                } catch (IllegalArgumentException | NullPointerException e) {
                    log.warn("잘못된 관리자 역할 문자열은 건너뜀 — memberId={}, requiredRole={}",
                            memberId, roleName);
                    continue;
                }
                if (adminUser.hasRole(required)) {
                    return toAdminUserInfo(adminUser);
                }
            }
        }

        log.warn("관리자 권한 부족 — memberId={}, adminRole={}, requiredRoles={}",
                memberId, adminUser.getAdminRole(), requiredRoles);
        throw new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }

    /**
     * memberId로 활성 AdminUser 엔티티를 조회한다 (내부 공통 메서드).
     *
     * @throws BusinessException ADMIN_USER_NOT_FOUND — admin_users에 레코드 없음
     * @throws BusinessException ADMIN_USER_DISABLED — 관리자 계정 비활성
     */
    private AdminUser findActiveAdminUser(Long memberId) {
        AdminUser adminUser = adminUserRepository.findByMemberId(memberId)
                .orElseThrow(() -> {
                    log.warn("관리자 계정 미등록 — memberId={}", memberId);
                    return new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
                });

        if (!adminUser.isActive()) {
            log.warn("비활성 관리자 접근 시도 — memberId={}, adminUserId={}, status={}",
                    memberId, adminUser.getId(), adminUser.getStatus());
            throw new BusinessException(ErrorCode.ADMIN_USER_DISABLED);
        }

        return adminUser;
    }

    /** AdminUser Entity를 외부 DTO로 변환. */
    private AdminUserInfo toAdminUserInfo(AdminUser adminUser) {
        return new AdminUserInfo(
                adminUser.getId(),
                adminUser.getMemberId(),
                adminUser.getAdminRole().name()
        );
    }
}
