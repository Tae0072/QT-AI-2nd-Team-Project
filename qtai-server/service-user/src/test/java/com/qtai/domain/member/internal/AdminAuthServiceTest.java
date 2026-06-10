package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.member.api.dto.AdminLoginRequest;
import com.qtai.domain.member.api.dto.AdminLoginResponse;
import com.qtai.domain.member.client.kakao.KakaoApiException;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import com.qtai.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AdminAuthService} 단위 테스트 — 카카오 검증·자동가입 금지·§5 이중검증·ADMIN 토큰 발급 흐름.
 */
class AdminAuthServiceTest {

    private static final String TOKEN = "kakao-access-token";

    private KakaoOAuthClient kakaoOAuthClient;
    private MemberRepository memberRepository;
    private JwtProvider jwtProvider;
    private RefreshTokenStore refreshTokenStore;
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private AdminAuthService service;

    @BeforeEach
    void setUp() {
        kakaoOAuthClient = mock(KakaoOAuthClient.class);
        memberRepository = mock(MemberRepository.class);
        jwtProvider = mock(JwtProvider.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        verifyAdminRoleUseCase = mock(VerifyAdminRoleUseCase.class);
        service = new AdminAuthService(kakaoOAuthClient, memberRepository, jwtProvider,
                refreshTokenStore, verifyAdminRoleUseCase, 1_209_600_000L);
    }

    private KakaoUserInfo kakaoUser(long id) {
        return new KakaoUserInfo(id, new KakaoUserInfo.KakaoAccount(
                "admin@test.dev", new KakaoUserInfo.KakaoAccount.Profile("n", null)));
    }

    private Member member(Member.MemberRole role, MemberStatus status) {
        Member m = mock(Member.class);
        given(m.getId()).willReturn(12L);
        given(m.getNickname()).willReturn("운영자");
        given(m.getRole()).willReturn(role);
        given(m.getStatus()).willReturn(status);
        return m;
    }

    @Test
    @DisplayName("관리자 회원이면 ADMIN 토큰을 발급하고 adminRole을 담아 반환한다")
    void 정상_관리자로그인() {
        Member m = member(Member.MemberRole.ADMIN, MemberStatus.ACTIVE);
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willReturn(kakaoUser(12L));
        given(memberRepository.findByKakaoId(12L)).willReturn(Optional.of(m));
        given(verifyAdminRoleUseCase.getActiveAdmin(12L))
                .willReturn(new AdminUserInfo(100L, 12L, "OPERATOR"));
        given(jwtProvider.issueAccessToken(12L, "ADMIN")).willReturn("acc");
        given(jwtProvider.issueRefreshToken(12L)).willReturn("ref");

        AdminLoginResponse response = service.adminLogin(new AdminLoginRequest(TOKEN));

        assertThat(response.accessToken()).isEqualTo("acc");
        assertThat(response.refreshToken()).isEqualTo("ref");
        assertThat(response.admin().memberId()).isEqualTo(12L);
        assertThat(response.admin().role()).isEqualTo("ADMIN");
        assertThat(response.admin().adminRole()).isEqualTo("OPERATOR");
        assertThat(response.admin().status()).isEqualTo("ACTIVE");
        verify(refreshTokenStore).save(eq(12L), eq("ref"), any());
    }

    @Test
    @DisplayName("카카오 검증 실패는 KAKAO_AUTH_FAILED")
    void 카카오_실패() {
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willThrow(new KakaoApiException("fail"));
        assertThatThrownBy(() -> service.adminLogin(new AdminLoginRequest(TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.KAKAO_AUTH_FAILED);
    }

    @Test
    @DisplayName("회원이 없으면(자동가입 안 함) ADMIN_USER_NOT_FOUND")
    void 회원없음() {
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willReturn(kakaoUser(12L));
        given(memberRepository.findByKakaoId(12L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.adminLogin(new AdminLoginRequest(TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("members.role!=ADMIN이면 ADMIN_USER_NOT_FOUND (§5 1차)")
    void 일반회원() {
        Member m = member(Member.MemberRole.USER, MemberStatus.ACTIVE);
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willReturn(kakaoUser(12L));
        given(memberRepository.findByKakaoId(12L)).willReturn(Optional.of(m));
        assertThatThrownBy(() -> service.adminLogin(new AdminLoginRequest(TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("정지된 회원은 MEMBER_SUSPENDED")
    void 정지회원() {
        Member m = member(Member.MemberRole.ADMIN, MemberStatus.SUSPENDED);
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willReturn(kakaoUser(12L));
        given(memberRepository.findByKakaoId(12L)).willReturn(Optional.of(m));
        assertThatThrownBy(() -> service.adminLogin(new AdminLoginRequest(TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_SUSPENDED);
    }

    @Test
    @DisplayName("admin_users 검증 실패(NOT_FOUND/DISABLED)는 그대로 전파한다")
    void admin검증_실패_전파() {
        Member m = member(Member.MemberRole.ADMIN, MemberStatus.ACTIVE);
        given(kakaoOAuthClient.getUserInfo(TOKEN)).willReturn(kakaoUser(12L));
        given(memberRepository.findByKakaoId(12L)).willReturn(Optional.of(m));
        given(verifyAdminRoleUseCase.getActiveAdmin(12L))
                .willThrow(new BusinessException(ErrorCode.ADMIN_USER_DISABLED));
        assertThatThrownBy(() -> service.adminLogin(new AdminLoginRequest(TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_USER_DISABLED);
    }
}
