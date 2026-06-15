package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListAdminMusicTrackUseCase {

    Page<AdminMusicTrackResponse> listAdmin(String status, Pageable pageable);
}
