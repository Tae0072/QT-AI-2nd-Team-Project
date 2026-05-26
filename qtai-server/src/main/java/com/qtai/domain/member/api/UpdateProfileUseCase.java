package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;

/**
 * 회원 프로필 수정 UseCase 포트.
 *
 * 본인만 수정 가능. 닉네임 중복 체크 필수.
 */
public interface UpdateProfileUseCase {

    MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request);
}
