package com.qtai.domain.admin.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.AdminAuthUseCase;
import com.qtai.domain.admin.api.dto.AdminLoginResult;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 관리자 자체 아이디/비밀번호 인증 서비스.
 *
 * <p>흐름: {@code username}으로 admin_users 조회 → BCrypt 비밀번호 검증 → 활성 확인 →
 * member 닉네임/상태 조회(member api UseCase) → ADMIN RS256 토큰 발급.
 *
 * <p>보안: 미존재 username/비밀번호 불일치는 동일한 {@code ADMIN_LOGIN_FAILED}(401)로
 * 응답해 계정 존재 여부 노출(enumeration)을 막는다. 평문 비밀번호·해시·토큰은 로그에 남기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService implements AdminAuthUseCase {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GetMemberUseCase getMemberUseCase;
    private final AdminLoginAttemptGuard loginAttemptGuard;
    private final AdminRefreshTokenStore refreshTokenStore;

    @Override
    public AdminLoginResult login(String username, String rawPassword) {
        // 브루트포스 방어: 연속 실패로 잠긴 계정은 시도 자체를 차단(429).
        loginAttemptGuard.assertNotLocked(username);

        AdminUser adminUser = adminUserRepository.findByUsername(username).orElse(null);
        if (adminUser == null
                || !StringUtils.hasText(adminUser.getPasswordHash())
                || !passwordEncoder.matches(rawPassword, adminUser.getPasswordHash())) {
            log.warn("관리자 로그인 실패 — 자격 불일치");
            loginAttemptGuard.recordFailure(username);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        if (!adminUser.isActive()) {
            log.warn("비활성 관리자 로그인 시도 — adminUserId={}, status={}",
                    adminUser.getId(), adminUser.getStatus());
            throw new BusinessException(ErrorCode.ADMIN_USER_DISABLED);
        }

        loginAttemptGuard.reset(username);
        return issueFor(adminUser);
    }

    @Override
    public AdminLoginResult refresh(String refreshToken) {
        Long memberId;
        try {
            memberId = jwtProvider.validateRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("관리자 토큰 갱신 실패 — 유효하지 않은 refresh token");
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        // 회전·재사용 검증: 저장된 현재 refresh token과 일치해야 한다(옛/탈취·재사용 토큰 거부).
        if (!refreshTokenStore.matches(memberId, refreshToken)) {
            log.warn("관리자 토큰 갱신 실패 — 회전/재사용된 refresh token (memberId={})", memberId);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        AdminUser adminUser = adminUserRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        if (!adminUser.isActive()) {
            throw new BusinessException(ErrorCode.ADMIN_USER_DISABLED);
        }

        return issueFor(adminUser);
    }

    private AdminLoginResult issueFor(AdminUser adminUser) {
        Long memberId = adminUser.getMemberId();
        MemberResponse member = getMemberUseCase.getMember(memberId);

        String accessToken = jwtProvider.issueAccessToken(memberId, ROLE_ADMIN);
        String refreshToken = jwtProvider.issueRefreshToken(memberId);
        // 새 refresh token 저장 → 기존 토큰 자동 무효화(회전).
        refreshTokenStore.save(memberId, refreshToken);

        return new AdminLoginResult(
                accessToken,
                refreshToken,
                new AdminLoginResult.Admin(
                        memberId,
                        member.nickname(),
                        ROLE_ADMIN,
                        adminUser.getAdminRole().name(),
                        member.status()
                )
        );
    }
}
