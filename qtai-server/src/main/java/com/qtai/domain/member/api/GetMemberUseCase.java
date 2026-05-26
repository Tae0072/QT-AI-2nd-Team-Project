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
}
