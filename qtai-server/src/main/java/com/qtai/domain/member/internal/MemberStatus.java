package com.qtai.domain.member.internal;

/**
 * 회원 상태 enum.
 *
 * - ACTIVE     : 정상 활동 회원
 * - SUSPENDED  : 관리자가 일시 정지 (로그인 차단)
 * - WITHDRAWN  : 탈퇴 — 익명화된 상태로 보존
 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN
}
