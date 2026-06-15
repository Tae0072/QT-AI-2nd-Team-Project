package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import org.springframework.data.domain.Pageable;

public interface ListAdminMusicTrackUseCase {

    /**
     * 관리자 배경음악 목록.
     *
     * @param status   노출 상태 필터(ACTIVE/HIDDEN), null이면 전체
     * @param category 분류 필터(BGM/HYMN), null이면 전체
     */
    AdminMusicTrackListResponse listAdmin(String status, String category, Pageable pageable);
}
