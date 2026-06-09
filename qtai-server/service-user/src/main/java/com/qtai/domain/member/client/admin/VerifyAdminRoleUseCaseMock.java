package com.qtai.domain.member.client.admin;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * admin 도메인 {@link VerifyAdminRoleUseCase} 임시 Mock (MSA 통합 전).
 *
 * <p>admin 도메인은 admin-server로 분리되어 service-user 클래스패스에 실제 구현체가 없다.
 * Day3 통합에서 RestClient 어댑터(admin-server 호출)로 교체하고 이 Mock은 삭제한다
 * (CLAUDE.md §4: {@code client/{타도메인명}/...UseCaseMock}).
 *
 * <p>안전 기본값: 모든 회원을 "관리자 아님"으로 간주해 {@code ADMIN_USER_NOT_FOUND}를 던진다.
 * 이렇게 하면 보존기간 만료 회원 정리({@code MemberRetentionPurgeService})가 관리자 연결로
 * 오인해 삭제를 막는 일이 없다(실제 admin 연동 전까지 회원 정리 흐름을 보수적으로 유지).
 */
@Slf4j
@Component
public class VerifyAdminRoleUseCaseMock implements VerifyAdminRoleUseCase {

    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        log.warn("[MOCK] admin.VerifyAdminRoleUseCase.getActiveAdmin — 통합 전 임시 구현(관리자 아님 처리): memberId={}", memberId);
        throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
    }

    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        log.warn("[MOCK] admin.VerifyAdminRoleUseCase.verifyRole — 통합 전 임시 구현(권한 없음 처리): memberId={}", memberId);
        throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
    }

    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, Collection<String> requiredRoles) {
        log.warn("[MOCK] admin.VerifyAdminRoleUseCase.verifyAnyRole — 통합 전 임시 구현(권한 없음 처리): memberId={}", memberId);
        throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
    }
}
