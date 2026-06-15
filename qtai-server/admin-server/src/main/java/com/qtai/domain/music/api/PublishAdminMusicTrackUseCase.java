package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;

public interface PublishAdminMusicTrackUseCase {

    AdminMusicTrackResponse publishAdmin(Long adminUserId, Long trackId);
}
