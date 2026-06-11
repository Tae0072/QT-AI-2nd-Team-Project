package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.MusicTrackResponse;
import java.util.List;

/**
 * 배경음악 목록 조회 UseCase 포트.
 */
public interface ListMusicTrackUseCase {

    /** 활성 음원 목록(메타데이터 + 스트리밍 URL). */
    List<MusicTrackResponse> listEnabled();
}
