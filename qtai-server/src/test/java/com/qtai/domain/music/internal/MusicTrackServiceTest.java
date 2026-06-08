package com.qtai.domain.music.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;
import com.qtai.domain.music.api.dto.MusicTrackResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MusicTrackService 단위 테스트.
 *
 * <p>projection(MusicTrackSummary/MusicTrackAudioView)은 익명 구현으로 스텁한다.
 */
@ExtendWith(MockitoExtension.class)
class MusicTrackServiceTest {

    @Mock
    private MusicTrackRepository musicTrackRepository;

    @InjectMocks
    private MusicTrackService musicTrackService;

    @Test
    void listEnabled_메타데이터와_streamUrl로_매핑한다() {
        given(musicTrackRepository.findByEnabledTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc())
                .willReturn(List.of(
                        summary(1L, "Peaceful", MusicCategory.BGM, "audio/mpeg", 1234L, 120, 0),
                        summary(2L, "Amazing Grace", MusicCategory.HYMN, "audio/mpeg", 4567L, 200, 1)));

        List<MusicTrackResponse> result = musicTrackService.listEnabled();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).category()).isEqualTo("BGM");
        assertThat(result.get(0).streamUrl()).isEqualTo("/api/v1/music/tracks/1/stream");
        assertThat(result.get(1).category()).isEqualTo("HYMN");
        assertThat(result.get(1).streamUrl()).isEqualTo("/api/v1/music/tracks/2/stream");
    }

    @Test
    void getAudio_정상_바이트를_반환한다() {
        given(musicTrackRepository.findEnabledAudioById(1L))
                .willReturn(Optional.of(audioView(new byte[]{1, 2, 3}, "audio/mpeg", 3L)));

        MusicTrackAudioResponse audio = musicTrackService.getAudio(1L);

        assertThat(audio.data()).hasSize(3);
        assertThat(audio.mimeType()).isEqualTo("audio/mpeg");
        assertThat(audio.byteSize()).isEqualTo(3L);
    }

    @Test
    void getAudio_없으면_RESOURCE_NOT_FOUND() {
        given(musicTrackRepository.findEnabledAudioById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> musicTrackService.getAudio(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("음원");
    }

    // ── projection 헬퍼 (익명 구현) ──

    private static MusicTrackSummary summary(Long id, String title, MusicCategory category,
                                             String mimeType, Long byteSize,
                                             Integer durationSec, Integer sortOrder) {
        return new MusicTrackSummary() {
            @Override public Long getId() { return id; }
            @Override public String getTitle() { return title; }
            @Override public MusicCategory getCategory() { return category; }
            @Override public String getMimeType() { return mimeType; }
            @Override public Long getByteSize() { return byteSize; }
            @Override public Integer getDurationSec() { return durationSec; }
            @Override public Integer getSortOrder() { return sortOrder; }
        };
    }

    private static MusicTrackAudioView audioView(byte[] data, String mimeType, Long byteSize) {
        return new MusicTrackAudioView() {
            @Override public byte[] getAudioData() { return data; }
            @Override public String getMimeType() { return mimeType; }
            @Override public Long getByteSize() { return byteSize; }
        };
    }
}
