package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;

/**
 * 회원 조회 UseCase 포트.
 *
 * <p>거의 모든 도메인이 이 포트로 회원 정보를 조회한다.
 * <p>도메인 간 결합도를 낮추기 위해 DTO 로 전달.
 */
public interface GetMemberUseCase {

    /** 내 정보 조회 (비공개 필드 포함). */
    MemberResponse getMember(Long memberId);

    /** 타 회원 공개 프로필 조회 (비공개 필드 제외). */
    MemberPublicResponse getMemberPublic(Long memberId);

    /**
     * 활성 회원 공개 프로필 일괄 조회 — 목록 화면(댓글 등) N+1 방지용.
     *
     * <p>{@link #getMemberPublic}과 달리 탈퇴·정지 회원은 예외 없이 결과에서
     * 제외된다 — 호출자는 누락된 id에 대해 자체 표시 정책(예: "탈퇴한 회원")으로
     * 폴백한다. (탈퇴 회원 1명이 목록 전체를 실패시키지 않도록 하는 계약,
     * MSA 분리 시 원격 벌크 호출 1회 계약의 기반)
     *
     * @param memberIds 조회할 회원 id 목록 (null/빈 목록이면 빈 결과)
     * @return 활성 회원의 공개 프로필 목록 (요청 순서 보장 없음)
     */
    java.util.List<MemberPublicResponse> getActivePublicProfiles(java.util.Collection<Long> memberIds);

    /**
     * 닉네임 정확 일치로 활성 회원 공개 프로필을 일괄 조회 — '#닉네임' 멘션 해석용.
     * 존재하지 않거나 비활성인 닉네임은 결과에서 제외된다.
     *
     * @param nicknames 조회할 닉네임 목록 (null/빈 목록이면 빈 결과)
     */
    java.util.List<MemberPublicResponse> resolveActiveByNicknames(java.util.Collection<String> nicknames);
}
