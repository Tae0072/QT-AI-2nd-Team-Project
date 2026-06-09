package com.qtai.common.security;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

/**
 * 컨트롤러 공통 인증 헬퍼.
 *
 * <p>{@code @AuthenticationPrincipal Long memberId}는 인증 필터가 통과시킨 경우에만 채워진다.
 * SecurityConfig가 {@code anyRequest().authenticated()}로 1차 차단하지만, 슬라이스 테스트나
 * 설정 변경으로 principal이 null로 들어오는 경로를 대비해 컨트롤러 진입부에서 한 번 더
 * 방어하고, 표준 {@link ErrorCode#UNAUTHORIZED} 예외로 통일한다.
 *
 * <p>관리자 권한(admin_role 이중검증)은 이 콘텐츠 서비스가 제공하지 않는다. 관리자 경로는
 * SecurityConfig에서 {@code /api/v1/admin/**} denyAll로 차단하며, 세부 권한 검증은
 * admin-server가 담당한다(2026-06-09 분리 설계). 따라서 이 헬퍼는 "인증 여부"만 책임진다.
 */
public final class AuthenticationSupport {

    private AuthenticationSupport() {
    }

    /**
     * 인증된 사용자 ID를 반환한다. principal이 없으면 {@link ErrorCode#UNAUTHORIZED}로 예외.
     *
     * @param memberId {@code @AuthenticationPrincipal}로 주입된 사용자 ID(없으면 null)
     * @return null이 아님이 보장된 사용자 ID
     */
    public static Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
