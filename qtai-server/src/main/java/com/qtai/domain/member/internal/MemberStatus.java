package com.qtai.domain.member.internal;

/**
 * 회원 상태. Member.MemberRole과 함께 member 도메인 내부에서만 사용.
 * MemberRole은 Member inner enum, MemberStatus는 별도 파일 — 상태값이 3종 이상이고
 * 도메인 서비스에서 독립적으로 참조하므로 별도 파일로 유지한다.
 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN
}
