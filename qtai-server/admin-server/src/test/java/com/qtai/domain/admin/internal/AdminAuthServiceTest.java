package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.dto.AdminLoginResult;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link AdminAuthService} 단위 테스트.
 *
 * <p>자체 아이디/비밀번호 로그인은 ADMIN 토큰을 발급하는 보안 민감 경로이므로,
 * 성공/실패(미존재·비밀번호 불일치·비활성)·토큰 갱신 분기를 모두 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    AdminUserRepository adminUserRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtProvider jwtProvider;
    @Mock
    GetMemberUseCase getMemberUseCase;

    @InjectMocks
    AdminAuthService adminAuthService;

    private AdminUser activeAdmin;

    @BeforeEach
    void setUp() {
        activeAdmin = AdminUser.builder()
                .memberId(10L)
                .adminRole(AdminRole.OPERATOR)
                .build();
        activeAdmin.assignCredentials("admin", "stored-hash");
    }

    private void stubIssue() {
        when(getMemberUseCase.getMember(10L)).thenReturn(
                new MemberResponse(10L, "개발관리자", null, null, "ACTIVE", "ADMIN", null, null));
        when(jwtProvider.issueAccessToken(10L, "ADMIN")).thenReturn("access-token");
        when(jwtProvider.issueRefreshToken(10L)).thenReturn("refresh-token");
    }

    @Test
    @DisplayName("로그인 성공 → ADMIN 토큰 + 요약 반환")
    void login_success() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("admin1234", "stored-hash")).thenReturn(true);
        stubIssue();

        AdminLoginResult result = adminAuthService.login("admin", "admin1234");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.admin().memberId()).isEqualTo(10L);
        assertThat(result.admin().nickname()).isEqualTo("개발관리자");
        assertThat(result.admin().role()).isEqualTo("ADMIN");
        assertThat(result.admin().adminRole()).isEqualTo("OPERATOR");
        assertThat(result.admin().status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("존재하지 않는 아이디 → ADMIN_LOGIN_FAILED(401)")
    void login_unknownUsername_fails() {
        when(adminUserRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login("nope", "admin1234"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED));
    }

    @Test
    @DisplayName("비밀번호 불일치 → ADMIN_LOGIN_FAILED(401)")
    void login_wrongPassword_fails() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login("admin", "wrong"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED));
    }

    @Test
    @DisplayName("비밀번호 해시 미설정(레거시 행) → ADMIN_LOGIN_FAILED(401)")
    void login_nullHash_fails() {
        AdminUser legacy = AdminUser.builder().memberId(11L).adminRole(AdminRole.OPERATOR).build();
        when(adminUserRepository.findByUsername("legacy")).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> adminAuthService.login("legacy", "admin1234"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED));
    }

    @Test
    @DisplayName("비활성 관리자 → ADMIN_USER_DISABLED(403)")
    void login_disabledAdmin_fails() {
        activeAdmin.disable();
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("admin1234", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.login("admin", "admin1234"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_USER_DISABLED));
    }

    @Test
    @DisplayName("refresh 성공 → 새 토큰 발급")
    void refresh_success() {
        when(jwtProvider.validateRefreshToken("refresh-token")).thenReturn(10L);
        when(adminUserRepository.findByMemberId(10L)).thenReturn(Optional.of(activeAdmin));
        stubIssue();

        AdminLoginResult result = adminAuthService.refresh("refresh-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.admin().adminRole()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("유효하지 않은 refresh token → ADMIN_LOGIN_FAILED(401)")
    void refresh_invalidToken_fails() {
        when(jwtProvider.validateRefreshToken("bad")).thenThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> adminAuthService.refresh("bad"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED));
    }
}
