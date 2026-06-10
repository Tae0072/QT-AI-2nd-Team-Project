package com.qtai.domain.member.internal;

import java.time.Duration;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.member.api.AdminLoginUseCase;
import com.qtai.domain.member.api.dto.AdminLoginRequest;
import com.qtai.domain.member.api.dto.AdminLoginResponse;
import com.qtai.domain.member.client.kakao.KakaoApiException;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import com.qtai.security.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 관리자 카카오 로그인 서비스 ({@link AdminLoginUseCase} 구현).
 *
 * <p>흐름(계약 §1, CLAUDE.md §5):
 * <ol>
 *   <li>카카오 access token 검증({@link KakaoOAuthClient}) — 실패 시 KAKAO_AUTH_FAILED.</li>
 *   <li>회원 조회({@code members.kakao_id}). <b>관리자 로그인은 자동 가입하지 않는다</b> — 없으면 관리자 아님(ADMIN_USER_NOT_FOUND).</li>
 *   <li>회원 상태 검증(SUSPENDED→MEMBER_SUSPENDED, 그 외 비ACTIVE→ADMIN_USER_NOT_FOUND).</li>
 *   <li>§5 1차: {@code members.role=ADMIN}.</li>
 *   <li>§5 2차: {@link VerifyAdminRoleUseCase#getActiveAdmin}(admin-server RestClient) — admin_users 활성·세부 역할. NOT_FOUND/DISABLED 전파.</li>
 *   <li>ADMIN 스코프 JWT(access/refresh) 발급 + Redis 저장. 만료는 사용자와 동일(access 30분/refresh 14일).</li>
 * </ol>
 *
 * <p>외부 호출(카카오·admin 검증)은 트랜잭션 밖에서 수행한다(읽기 1건이라 DB 커넥션을 HTTP 동안 잡지 않는다).
 * 토큰·시크릿은 로그에 남기지 않는다(§9).
 */
@Slf4j
@Service
public class AdminAuthService implements AdminLoginUseCase {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final long refreshExpiryMs;

    public AdminAuthService(
            KakaoOAuthClient kakaoOAuthClient,
            MemberRepository memberRepository,
            JwtProvider jwtProvider,
            RefreshTokenStore refreshTokenStore,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            @Value("${security.jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    @Override
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        // 1. 카카오 사용자 정보 조회
        KakaoUserInfo kakaoUser;
        try {
            kakaoUser = kakaoOAuthClient.getUserInfo(request.kakaoAccessToken());
        } catch (KakaoApiException e) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        // 2. 회원 조회 — 관리자 로그인은 자동 가입하지 않는다(없으면 관리자 아님)
        Member member = memberRepository.findByKakaoId(kakaoUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));

        // 3. 회원 상태 검증
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            // 탈퇴 등 비활성 — 관리자 로그인 거부
            throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
        }

        // 4. §5 1차 검증: members.role=ADMIN
        if (!"ADMIN".equals(member.getRole().name())) {
            throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
        }

        // 5. §5 2차 검증: admin_users 활성 + 세부 역할 (admin-server RestClient)
        AdminUserInfo admin = verifyAdminRoleUseCase.getActiveAdmin(member.getId());

        // 6. ADMIN 스코프 토큰 발급 + Redis 저장
        String accessToken = jwtProvider.issueAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtProvider.issueRefreshToken(member.getId());
        refreshTokenStore.save(member.getId(), refreshToken, Duration.ofMillis(refreshExpiryMs));

        log.info("관리자 로그인 성공: memberId={}, adminRole={}", member.getId(), admin.adminRole());

        return new AdminLoginResponse(
                accessToken,
                refreshToken,
                new AdminLoginResponse.AdminSummary(
                        member.getId(),
                        member.getNickname(),
                        member.getRole().name(),
                        admin.adminRole(),
                        member.getStatus().name()
                )
        );
    }
}
