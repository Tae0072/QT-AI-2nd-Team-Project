package com.qtai.common.security;

/**
 * 서비스 간 시스템(배치·스케줄러) 토큰의 공통 claim 상수.
 *
 * <p>사용자 RS256 토큰과 분리된 HS256 단명 SYSTEM_BATCH 토큰에서 사용한다.
 * 발급({@link SystemTokenProvider})과 검증({@link SystemTokenValidator})이 동일 규약을 쓰도록 한 곳에 모은다.
 */
final class SystemTokenClaims {

    /** 시스템 토큰의 subject(회원 ID 자리) — 사용자가 아니므로 0으로 고정. */
    static final String SUBJECT = "0";

    /** 시스템 주체 memberId(0L) — SecurityContext principal 값. */
    static final long SYSTEM_MEMBER_ID = 0L;

    /** 시스템 토큰의 role claim 값. */
    static final String ROLE = "SYSTEM_BATCH";

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TOKEN_TYPE = "type";

    /** 시스템 토큰임을 표시하는 type claim 값(사용자/refresh 토큰과 구분). */
    static final String TOKEN_TYPE_SYSTEM = "system";

    private SystemTokenClaims() {
    }
}
