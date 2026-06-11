package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.SettingsResponse;

public interface GetSettingsUseCase {

    SettingsResponse getSettings(Long memberId);
}
