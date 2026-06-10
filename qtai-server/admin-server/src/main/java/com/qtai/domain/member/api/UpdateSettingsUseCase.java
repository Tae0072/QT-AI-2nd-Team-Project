package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.SettingsResponse;
import com.qtai.domain.member.api.dto.SettingsUpdateRequest;

public interface UpdateSettingsUseCase {

    SettingsResponse updateSettings(Long memberId, SettingsUpdateRequest request);
}
