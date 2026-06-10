package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;

public interface ChangeNicknameUseCase {

    MemberResponse changeNickname(Long memberId, NicknameChangeRequest request);

    boolean isNicknameAvailable(String nickname);
}
