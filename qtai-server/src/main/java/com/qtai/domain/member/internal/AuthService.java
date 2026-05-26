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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;

/**
 * 인증 서비스 — 로그인, 로그아웃, 토큰 갱신.
 *
 * 로그인 흐름:
 * 1. Flutter SDK가 카카오 access token 발급
 * 2. POST /api/v1/auth/kakao로 전달
 * 3. KakaoOAuthClient로 사용자 정보 조회 (트랜잭션 밖)
 * 4. Member 조회 또는 자동 가입 — TransactionTemplate으로 프로그래매틱 트랜잭션 관리
 * 5. JWT access/refresh token 발급 + Redis 저장
 */
@Slf4j
@Service
public class AuthService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final MemberAuthProviderRepository authProviderRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProvider jwtProvider;
    private final TransactionTemplate transactionTemplate;
    private final long refreshExpiryMs;

    public AuthService(
            KakaoOAuthClient kakaoOAuthClient,
            MemberRepository memberRepository,
            MemberAuthProviderRepository authProviderRepository,
            RefreshTokenStore refreshTokenStore,
            JwtProvider jwtProvider,
            TransactionTemplate transactionTemplate,
            @Value("${security.jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.memberRepository = memberRepository;
        this.authProviderRepository = authProviderRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.jwtProvider = jwtProvider;
        this.transactionTemplate = transactionTemplate;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 카카오 사용자 정보 조회 — 트랜잭션 밖에서 외부 HTTP 호출 (DB 커넥션 점유 방지)
        KakaoUserInfo kakaoUser;
        try {
            kakaoUser = kakaoOAuthClient.getUserInfo(request.kakaoAccessToken());
        } catch (KakaoApiException e) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        // 2. DB 작업 — TransactionTemplate으로 프로그래매틱 트랜잭션 (self-invocation 방지)
        Member member = transactionTemplate.execute(status -> {
            Long kakaoId = kakaoUser.id();

            Member found = memberRepository.findByKakaoId(kakaoId)
                    .orElseGet(() -> registerNewMember(kakaoUser));

            // 탈퇴/정지 회원 검증
            if (found.getStatus() == MemberStatus.WITHDRAWN) {
                throw new BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
            }
            if (found.getStatus() == MemberStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
            }
            return found;
        });

        // 3. JWT 발급 + Redis 저장 (트랜잭션 밖)
        String accessToken = jwtProvider.issueAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtProvider.issueRefreshToken(member.getId());
        refreshTokenStore.save(member.getId(), refreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("로그인 성공: memberId={}, kakaoId={}, isNew={}", member.getId(), kakaoUser.id(),
                member.getNicknameChangedAt() == null);

        return buildLoginResponse(member, accessToken, refreshToken);
    }

    @Override
    public void logout(Long memberId) {
        refreshTokenStore.delete(memberId);
        log.info("로그아웃 완료: memberId={}", memberId);
    }

    @Override
    @Transactional(readOnly = true)
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
            log.warn("비활성 회원 토큰 갱신 시도: memberId={}, status={}", memberId, member.getStatus());
            refreshTokenStore.delete(memberId);
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 4. 새 토큰 쌍 발급 (rotation)
        String newAccessToken = jwtProvider.issueAccessToken(memberId, member.getRole().name());
        String newRefreshToken = jwtProvider.issueRefreshToken(memberId);
        refreshTokenStore.save(memberId, newRefreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("토큰 갱신 완료: memberId={}", memberId);

        return buildLoginResponse(member, newAccessToken, newRefreshToken);
    }

    // -------------------------------------------------------------------------
    // private
    // -------------------------------------------------------------------------

    /**
     * LoginResponse 생성 헬퍼.
     */
    private LoginResponse buildLoginResponse(Member member, String accessToken, String refreshToken) {
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

    /**
     * 첫 로그인 시 자동 회원 가입.
     * nickname은 임시값("user_{kakaoId}")으로 설정하며, 온보딩 화면에서 변경한다.
     */
    private Member registerNewMember(KakaoUserInfo kakaoUser) {
        String tempNickname = "user_" + kakaoUser.id();
        // 닉네임 충돌 시 UUID suffix 추가 (동시 가입 안전)
        if (memberRepository.existsByNickname(tempNickname)) {
            tempNickname = "user_" + kakaoUser.id() + "_"
                    + UUID.randomUUID().toString().substring(0, 8);
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
