package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackCommand;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;

public interface UpdateAdminMusicTrackUseCase {

    AdminMusicTrackResponse updateAdmin(Long adminUserId, Long trackId, AdminMusicTrackCommand command);
}
