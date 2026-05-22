package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberResponse;

/**
 * 회원 조회 UseCase 포트.
 *
 * 거의 모든 도메인이 이 포트로 회원 정보를 조회한다.
 * 도메인 간 결합도를 낮추기 위해 MemberResponse DTO로 전달.
 */
public interface GetMemberUseCase {

    MemberResponse getMember(Long memberId);
}
