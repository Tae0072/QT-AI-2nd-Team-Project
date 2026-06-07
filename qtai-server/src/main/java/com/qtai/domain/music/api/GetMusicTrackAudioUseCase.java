package com.qtai.domain.music.api;

import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;

/**
 * 배경음악 스트리밍(바이트) 조회 UseCase 포트.
 */
public interface GetMusicTrackAudioUseCase {

    /** 활성 음원 1건의 바이트를 조회한다. 없으면 RESOURCE_NOT_FOUND. */
    MusicTrackAudioResponse getAudio(Long trackId);
}
