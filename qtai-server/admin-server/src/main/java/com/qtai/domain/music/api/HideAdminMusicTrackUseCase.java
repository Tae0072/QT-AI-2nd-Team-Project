package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;

public interface HideAdminMusicTrackUseCase {

    AdminMusicTrackResponse hideAdmin(Long adminUserId, Long trackId);
}
