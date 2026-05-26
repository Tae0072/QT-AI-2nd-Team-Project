package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.LogoutUseCase;
import com.qtai.domain.member.api.RefreshTokenUseCase;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.api.dto.RefreshTokenRequest;
import com.qtai.domain.member.client.kakao.KakaoApiException;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import com.qtai.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 인증 서비스 — 로그인, 로그아웃, 토큰 갱신.
 *
 * 로그인 흐름:
 * 1. Flutter SDK가 카카오 access token 발급
 * 2. POST /api/v1/auth/kakao로 전달
 * 3. KakaoOAuthClient로 사용자 정보 조회
 * 4. Member 조회 또는 자동 가입 (첫 로그인)
 * 5. JWT access/refresh token 발급 + Redis 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final MemberAuthProviderRepository authProviderRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProvider jwtProvider;

    @Value("${security.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 카카오 사용자 정보 조회 — 트랜잭션 밖에서 외부 HTTP 호출 (DB 커넥션 점유 방지)
        KakaoUserInfo kakaoUser;
        try {
            kakaoUser = kakaoOAuthClient.getUserInfo(request.kakaoAccessToken());
        } catch (KakaoApiException e) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        // 2. DB 작업 + 토큰 발급 (트랜잭션 내부)
        return loginInternal(kakaoUser);
    }

    @Transactional
    protected LoginResponse loginInternal(KakaoUserInfo kakaoUser) {
        Long kakaoId = kakaoUser.id();

        // 기존 회원 조회 또는 신규 가입
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> registerNewMember(kakaoUser));

        // 탈퇴/정지 회원 검증
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }

        // JWT 발급
        String accessToken = jwtProvider.issueAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtProvider.issueRefreshToken(member.getId());

        // Refresh token Redis 저장
        refreshTokenStore.save(member.getId(), refreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("로그인 성공: memberId={}, kakaoId={}, isNew={}", member.getId(), kakaoId,
                member.getNicknameChangedAt() == null);

        // 응답 생성
        boolean onboardingRequired = member.getNicknameChangedAt() == null
                && member.getNickname().startsWith("user_");
        return new LoginResponse(
                accessToken,
                refreshToken,
                new LoginResponse.MemberSummary(
                        member.getId(),
                        member.getNickname(),
                        member.getRole().name(),
                        member.getStatus().name(),
                        onboardingRequired
                )
        );
    }

    @Override
    @Transactional
    public void logout(Long memberId) {
        refreshTokenStore.delete(memberId);
        log.info("로그아웃 완료: memberId={}", memberId);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        // 1. Refresh token 검증 (서명 + 만료 + type)
        Long memberId;
        try {
            memberId = jwtProvider.validateRefreshToken(request.refreshToken());
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. Redis에 저장된 토큰과 일치하는지 확인 (탈취 방지)
        String storedToken = refreshTokenStore.find(memberId);
        if (storedToken == null || !storedToken.equals(request.refreshToken())) {
            // 토큰 불일치 → 잠재적 탈취, 해당 회원의 모든 세션 무효화
            refreshTokenStore.delete(memberId);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 회원 존재/상태 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            refreshTokenStore.delete(memberId);
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 4. 새 토큰 쌍 발급 (rotation)
        String newAccessToken = jwtProvider.issueAccessToken(memberId, member.getRole().name());
        String newRefreshToken = jwtProvider.issueRefreshToken(memberId);
        refreshTokenStore.save(memberId, newRefreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("토큰 갱신 완료: memberId={}", memberId);

        boolean onboardingRequired = member.getNicknameChangedAt() == null
                && member.getNickname().startsWith("user_");
        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                new LoginResponse.MemberSummary(
                        member.getId(),
                        member.getNickname(),
                        member.getRole().name(),
                        member.getStatus().name(),
                        onboardingRequired
                )
        );
    }

    // -------------------------------------------------------------------------
    // private
    // -------------------------------------------------------------------------

    /**
     * 첫 로그인 시 자동 회원 가입.
     * nickname은 임시값("user_{kakaoId}")으로 설정하며, 온보딩 화면에서 변경한다.
     */
    private Member registerNewMember(KakaoUserInfo kakaoUser) {
        String tempNickname = "user_" + kakaoUser.id();
        // 혹시 닉네임 충돌 시 suffix 추가
        if (memberRepository.existsByNickname(tempNickname)) {
            tempNickname = "user_" + kakaoUser.id() + "_" + System.currentTimeMillis() % 10000;
        }

        Member member = Member.builder()
                .kakaoId(kakaoUser.id())
                .email(kakaoUser.getEmail())
                .nickname(tempNickname)
                .profileImageUrl(kakaoUser.getProfileImageUrl())
                .build();
        member = memberRepository.save(member);

        // auth_provider 연동 정보 저장
        MemberAuthProvider authProvider = MemberAuthProvider.builder()
                .memberId(member.getId())
                .provider("KAKAO")
                .providerUserId(String.valueOf(kakaoUser.id()))
                .build();
        authProviderRepository.save(authProvider);

        log.info("신규 회원 가입: memberId={}, kakaoId={}", member.getId(), kakaoUser.id());
        return member;
    }
}
