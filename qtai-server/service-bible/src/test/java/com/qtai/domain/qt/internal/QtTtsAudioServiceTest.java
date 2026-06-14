package com.qtai.domain.qt.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.dto.QtPassageAudioResponse;
import com.qtai.domain.qt.client.tts.TtsClient;
import com.qtai.domain.qt.client.tts.TtsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** QT 본문 TTS 지연 캐시 서비스 단위 테스트 — 캐시 히트/생성/검증. */
@ExtendWith(MockitoExtension.class)
class QtTtsAudioServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private QtPassageAudioRepository audioRepository;
    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private QtPassageVerseRepository qtPassageVerseRepository;
    @Mock private GetBibleVerseUseCase getBibleVerseUseCase;
    @Mock private TtsClient ttsClient;
    @Mock private TtsProperties ttsProperties;

    private QtTtsAudioService service() {
        return new QtTtsAudioService(audioRepository, qtPassageRepository, qtPassageVerseRepository,
                getBibleVerseUseCase, ttsClient, ttsProperties, CLOCK);
    }

    @Test
    void 잘못된_id면_INVALID_INPUT() {
        assertThatThrownBy(() -> service().getAudio(0L, "선희 (여성)"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 캐시_히트면_생성없이_반환() {
        QtPassageAudioView view = mockView("audio/mpeg", new byte[]{1, 2, 3});
        when(audioRepository.findAudio(5L, "선희 (여성)")).thenReturn(Optional.of(view));

        QtPassageAudioResponse res = service().getAudio(5L, "선희 (여성)");

        assertThat(res.mimeType()).isEqualTo("audio/mpeg");
        assertThat(res.data()).containsExactly(1, 2, 3);
        verify(ttsClient, never()).generate(any(), any());
        verify(qtPassageRepository, never()).findById(any());
    }

    @Test
    void 캐시_미스면_본문생성하고_저장() {
        when(audioRepository.findAudio(eq(5L), any())).thenReturn(Optional.empty());

        QtPassage passage = org.mockito.Mockito.mock(QtPassage.class);
        when(passage.getStatus()).thenReturn(QtPassageStatus.ACTIVE);
        when(passage.getQtDate()).thenReturn(LocalDate.of(2026, 6, 14));
        when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));

        QtPassageVerse v1 = org.mockito.Mockito.mock(QtPassageVerse.class);
        when(v1.getBibleVerseId()).thenReturn(101L);
        when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(5L))
                .thenReturn(List.of(v1));
        when(getBibleVerseUseCase.getVerses(anyList())).thenReturn(List.of(
                new BibleVerseResponse(101L, "GEN", 9, 1, "하나님이 노아와 그 아들들에게 복을 주시며", null)));
        when(ttsClient.generate(any(), eq("선희 (여성)")))
                .thenReturn(new TtsClient.GeneratedAudio(new byte[]{9, 9}, "audio/mpeg"));

        QtPassageAudioResponse res = service().getAudio(5L, "선희 (여성)");

        assertThat(res.data()).containsExactly(9, 9);
        verify(audioRepository).save(any(QtPassageAudio.class));
    }

    private static QtPassageAudioView mockView(String mime, byte[] data) {
        QtPassageAudioView view = org.mockito.Mockito.mock(QtPassageAudioView.class);
        when(view.getMimeType()).thenReturn(mime);
        when(view.getAudioData()).thenReturn(data);
        return view;
    }
}
