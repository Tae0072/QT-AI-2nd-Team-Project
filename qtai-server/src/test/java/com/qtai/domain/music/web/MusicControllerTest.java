package com.qtai.domain.music.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.music.api.GetMusicTrackAudioUseCase;
import com.qtai.domain.music.api.ListMusicTrackUseCase;
import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;
import com.qtai.domain.music.api.dto.MusicTrackResponse;
import com.qtai.security.JwtAuthenticationFilter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MusicController MockMvc 슬라이스 테스트.
 *
 * <p>Security 필터 비활성화(addFilters=false). 인증 주체는 SecurityContext 에 직접 세팅.
 */
@WebMvcTest(MusicController.class)
@AutoConfigureMockMvc(addFilters = false)
class MusicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ListMusicTrackUseCase listMusicTrackUseCase;

    @MockBean
    private GetMusicTrackAudioUseCase getMusicTrackAudioUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listTracks_200_활성_음원_목록() throws Exception {
        when(listMusicTrackUseCase.listEnabled()).thenReturn(List.of(
                new MusicTrackResponse(1L, "Peaceful", "BGM", "audio/mpeg",
                        1234L, 120, 0, "/api/v1/music/tracks/1/stream")));

        mockMvc.perform(get("/api/v1/music/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Peaceful"))
                .andExpect(jsonPath("$.data[0].category").value("BGM"))
                .andExpect(jsonPath("$.data[0].streamUrl").value("/api/v1/music/tracks/1/stream"));
    }

    @Test
    void streamTrack_200_음원_바이트() throws Exception {
        when(getMusicTrackAudioUseCase.getAudio(1L)).thenReturn(
                new MusicTrackAudioResponse(new byte[]{1, 2, 3}, "audio/mpeg", 3L));

        mockMvc.perform(get("/api/v1/music/tracks/1/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("audio/mpeg")));
    }

    @Test
    void streamTrack_404_없는_음원() throws Exception {
        when(getMusicTrackAudioUseCase.getAudio(99L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/music/tracks/99/stream"))
                .andExpect(status().isNotFound());
    }
}
