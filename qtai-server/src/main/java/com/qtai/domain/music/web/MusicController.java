package com.qtai.domain.music.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.music.api.GetMusicTrackAudioUseCase;
import com.qtai.domain.music.api.ListMusicTrackUseCase;
import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;
import com.qtai.domain.music.api.dto.MusicTrackResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배경음악 REST 엔드포인트.
 *
 * <ul>
 *   <li>GET /api/v1/music/tracks — 활성 음원 목록(메타데이터 + streamUrl)</li>
 *   <li>GET /api/v1/music/tracks/{id}/stream — 음원 바이트 스트리밍</li>
 * </ul>
 *
 * <p>인가: SecurityConfig 의 {@code anyRequest().authenticated()} 로 보호된다(로그인 필요).
 */
@RestController
@RequiredArgsConstructor
public class MusicController {

    private final ListMusicTrackUseCase listMusicTrackUseCase;
    private final GetMusicTrackAudioUseCase getMusicTrackAudioUseCase;

    /** GET /api/v1/music/tracks — 활성 음원 목록. */
    @GetMapping("/api/v1/music/tracks")
    public ResponseEntity<ApiResponse<List<MusicTrackResponse>>> listTracks() {
        List<MusicTrackResponse> tracks = listMusicTrackUseCase.listEnabled();
        return ResponseEntity.ok(ApiResponse.success(tracks));
    }

    /** GET /api/v1/music/tracks/{id}/stream — 음원 바이트 스트리밍(전체 바이트). */
    @GetMapping("/api/v1/music/tracks/{id}/stream")
    public ResponseEntity<byte[]> streamTrack(@PathVariable("id") Long id) {
        MusicTrackAudioResponse audio = getMusicTrackAudioUseCase.getAudio(id);
        byte[] data = audio.data();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.mimeType()))
                .contentLength(data.length)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(data);
    }
}
