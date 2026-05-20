package com.qtai.domain.member.api;

/**
 * 회원 조회 UseCase 포트.
 *
 * 거의 모든 도메인이 이 포트로 회원 정보를 조회한다 (qt/study/note/sharing/report/
 * notification/praise/mission/ai/admin). 도메인 간 결합도를 낮추기 위해 Member
 * 엔티티를 직접 노출하지 않고 MemberResponse DTO로 전달한다.
 */
public interface GetMemberUseCase {

    // TODO: MemberResponse getMember(Long memberId);
    //       없으면 throw BusinessException(MEMBER_NOT_FOUND)
}
