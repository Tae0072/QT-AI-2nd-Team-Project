package com.qtai.domain.music.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.music.api.GetMusicTrackAudioUseCase;
import com.qtai.domain.music.api.ListMusicTrackUseCase;
import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;
import com.qtai.domain.music.api.dto.MusicTrackResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배경음악 도메인 서비스.
 *
 * <p>읽기 전용. projection → DTO 변환은 이 서비스에서 수행한다(praise 패턴 일치).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicTrackService implements ListMusicTrackUseCase, GetMusicTrackAudioUseCase {

    private final MusicTrackRepository musicTrackRepository;

    @Override
    public List<MusicTrackResponse> listEnabled() {
        return musicTrackRepository
                .findByEnabledTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MusicTrackAudioResponse getAudio(Long trackId) {
        MusicTrackAudioView audio = musicTrackRepository.findEnabledAudioById(trackId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "음원을 찾을 수 없습니다: " + trackId));
        return new MusicTrackAudioResponse(
                audio.getAudioData(), audio.getMimeType(), audio.getByteSize());
    }

    private MusicTrackResponse toResponse(MusicTrackSummary s) {
        return new MusicTrackResponse(
                s.getId(),
                s.getTitle(),
                s.getCategory().name(),
                s.getMimeType(),
                s.getByteSize(),
                s.getDurationSec(),
                s.getSortOrder(),
                "/api/v1/music/tracks/" + s.getId() + "/stream"
        );
    }
}
