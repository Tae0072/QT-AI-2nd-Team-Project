package com.qtai.domain.member.api;

/**
 * 회원 프로필 수정 UseCase 포트.
 *
 * 본인만 수정 가능. 닉네임 중복 체크 필수.
 */
public interface UpdateProfileUseCase {

    // TODO: MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request);
    //       닉네임 중복이면 throw BusinessException(INVALID_INPUT, "닉네임 중복")
}
