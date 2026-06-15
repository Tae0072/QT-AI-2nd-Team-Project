package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackCommand;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;

public interface CreateAdminMusicTrackUseCase {

    AdminMusicTrackResponse createAdmin(Long adminUserId, AdminMusicTrackCommand command);
}
