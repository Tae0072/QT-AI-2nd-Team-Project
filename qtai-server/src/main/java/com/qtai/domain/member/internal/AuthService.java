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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 인증 서비스 — 로그인, 로그아웃, 토큰 갱신.
 *
 * <p>로그인 흐름:
 * <ol>
 *   <li>Flutter SDK가 카카오 access token 발급</li>
 *   <li>POST /api/v1/auth/kakao로 전달</li>
 *   <li>KakaoOAuthClient로 사용자 정보 조회 (트랜잭션 밖)</li>
 *   <li>Member 조회 또는 자동 가입 — TransactionTemplate으로 프로그래매틱 트랜잭션 관리</li>
 *   <li>탈퇴(WITHDRAWN) 회원이면 재활성화 — 개인정보 2년 보존 정책에 따른 계정 복구</li>
 *   <li>JWT access/refresh token 발급 + Redis 저장</li>
 * </ol>
 *
 * <p>동시성 처리:
 * 같은 kakaoId로 첫 로그인이 동시에 들어오면 unique constraint 위반이 발생한다.
 * 이때 첫 트랜잭션은 롤백되고, 새 트랜잭션에서 이미 생성된 회원을 재조회한다.
 * (Hibernate 세션 오염 방지를 위해 catch는 transactionTemplate 바깥에 위치)
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
        //    DataIntegrityViolationException은 트랜잭션 바깥에서 catch하여
        //    Hibernate 세션 오염 없이 새 트랜잭션에서 재조회한다.
        Member member;
        try {
            member = Objects.requireNonNull(
                    transactionTemplate.execute(status -> {
                        Long kakaoId = kakaoUser.id();

                        Member found = memberRepository.findByKakaoId(kakaoId)
                                .orElseGet(() -> registerNewMember(kakaoUser));

                        reactivateIfWithdrawn(found, kakaoUser);
                        validateMemberStatus(found);
                        return found;
                    }),
                    "트랜잭션에서 Member 조회/생성 실패"
            );
        } catch (DataIntegrityViolationException e) {
            // 동시 가입 경합: kakaoId 또는 닉네임 unique constraint 위반
            // 다른 스레드가 먼저 생성 완료했으므로 새 트랜잭션에서 재조회.
            // 주의: 이 블록은 login()의 재시도 경로다 — refresh 경로가 아니므로
            // 여기서의 reactivateIfWithdrawn 호출은 "refresh는 재활성화 차단" 정책과
            // 무관하다 (refresh()는 재활성화 없이 MEMBER_ALREADY_WITHDRAWN 유지).
            log.info("동시 가입 경합 감지, 재조회: kakaoId={}", kakaoUser.id());
            member = Objects.requireNonNull(
                    transactionTemplate.execute(status -> {
                        Member found = memberRepository.findByKakaoId(kakaoUser.id())
                                .orElseThrow(() -> new BusinessException(ErrorCode.KAKAO_AUTH_FAILED));
                        reactivateIfWithdrawn(found, kakaoUser);
                        validateMemberStatus(found);
                        return found;
                    }),
                    "재조회 트랜잭션에서 Member 조회 실패"
            );
        }

        // 3. JWT 발급 + Redis 저장 (트랜잭션 밖)
        String accessToken = jwtProvider.issueAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtProvider.issueRefreshToken(member.getId());
        refreshTokenStore.save(member.getId(), refreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("로그인 성공: memberId={}, kakaoId={}", member.getId(), kakaoUser.id());

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

        // 3. 회원 존재/상태 확인 — 상태별 에러코드를 구분하여 클라이언트가 적절히 처리 가능
        //    정책: 재활성화는 명시적 재로그인(login)에서만 수행한다. refresh 경로는
        //    탈퇴 회원을 재활성화하지 않고 차단한다 — 탈퇴 후 남은 토큰으로
        //    사용자가 모르는 사이 계정이 복구되는 것을 방지(2026-06-05 결정).
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            log.warn("비활성 회원 토큰 갱신 시도: memberId={}, status={}", memberId, member.getStatus());
            refreshTokenStore.delete(memberId);
            if (member.getStatus() == MemberStatus.WITHDRAWN) {
                throw new BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
            }
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
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
     * 탈퇴 회원 재활성화 — 개인정보 2년 보존 정책에 따라 보존 중 재로그인 시 기존 계정을 복구한다.
     *
     * <p>member_auth_providers 연동 row가 보존되어 있으므로 신규 가입 경로를 타지 않는다
     * (신규 insert 시 (provider, provider_user_id) UNIQUE 충돌 — M0009 원인).
     */
    private void reactivateIfWithdrawn(Member member, KakaoUserInfo kakaoUser) {
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            member.reactivate(kakaoUser.getEmail(), kakaoUser.getProfileImageUrl());
            log.info("탈퇴 회원 재활성화: memberId={}", member.getId());
        }
    }

    /**
     * 회원 상태 검증 — 정지 회원은 로그인 차단.
     *
     * <p>탈퇴 회원은 차단하지 않는다 — 2년 보존 정책에 따라
     * {@link #reactivateIfWithdrawn(Member, KakaoUserInfo)}가 먼저 재활성화한다.
     * (refresh 경로는 재활성화 없이 MEMBER_ALREADY_WITHDRAWN을 유지한다)
     */
    private void validateMemberStatus(Member member) {
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
    }

    /**
     * LoginResponse 생성 헬퍼.
     *
     * <p>onboardingRequired 판별: nicknameChangedAt이 null이면 온보딩 미완료로 판단한다.
     * 온보딩에서 닉네임을 설정하면 nicknameChangedAt이 기록되므로, 닉네임 prefix와 무관하게
     * 온보딩 완료 여부를 정확히 판별할 수 있다.
     */
    private LoginResponse buildLoginResponse(Member member, String accessToken, String refreshToken) {
        boolean onboardingRequired = member.getNicknameChangedAt() == null;
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
     *
     * <p>동시 요청 시 unique constraint 위반은 호출자(login)에서 catch하여
     * 새 트랜잭션에서 재조회한다.
     */
    private Member registerNewMember(KakaoUserInfo kakaoUser) {
        String tempNickname = generateTempNickname(kakaoUser.id());

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

    /** members.nickname 컬럼 길이(VARCHAR(20)). */
    private static final int NICKNAME_MAX_LENGTH = 20;
    /** 임시 닉네임 접두사 — 닉네임 변경 API에서 예약어로 차단한다. */
    static final String TEMP_NICKNAME_PREFIX = "user_";

    /**
     * 항상 20자 이내이면서 닉네임 UNIQUE를 만족하는 임시 닉네임을 만든다.
     *
     * <p>버그 수정(2026-06-05): 기존 충돌 fallback "user_{10자리kakaoId}_{UUID8}"는
     * 24자라 VARCHAR(20)을 초과해 INSERT가 "Data too long"으로 실패했다. 이 실패는
     * DataIntegrityViolationException으로 잡혀 login()의 kakaoId 재조회로 넘어가는데,
     * 정작 행이 안 만들어졌으므로 KAKAO_AUTH_FAILED(401)로 둔갑하고 해당 사용자는
     * 영영 가입할 수 없었다. 길이를 보장하는 생성기로 교체한다.
     */
    private String generateTempNickname(long kakaoId) {
        String base = truncate(TEMP_NICKNAME_PREFIX + kakaoId, NICKNAME_MAX_LENGTH);
        if (!memberRepository.existsByNickname(base)) {
            return base;
        }
        // 충돌 시: 6자리 랜덤 suffix를 붙이되 base를 잘라 전체 20자 이내를 유지
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            String candidate = truncate(base, NICKNAME_MAX_LENGTH - suffix.length()) + suffix;
            if (!memberRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        // 극히 드문 연속 충돌 — 전부 랜덤(접두사 유지)으로 최종 시도
        return truncate(TEMP_NICKNAME_PREFIX, NICKNAME_MAX_LENGTH)
                + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, NICKNAME_MAX_LENGTH - TEMP_NICKNAME_PREFIX.length());
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
