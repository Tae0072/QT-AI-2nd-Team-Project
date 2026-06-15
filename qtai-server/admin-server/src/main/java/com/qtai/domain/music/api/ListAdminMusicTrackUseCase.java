package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import org.springframework.data.domain.Pageable;

public interface ListAdminMusicTrackUseCase {

    AdminMusicTrackListResponse listAdmin(String status, Pageable pageable);
}
