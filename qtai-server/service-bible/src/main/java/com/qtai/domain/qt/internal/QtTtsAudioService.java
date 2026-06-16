package com.qtai.domain.qt.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageAudioUseCase;
import com.qtai.domain.qt.api.dto.QtPassageAudioResponse;
import com.qtai.domain.qt.client.tts.TtsClient;
import com.qtai.domain.qt.client.tts.TtsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 오늘 QT 본문 TTS 음성 지연 캐시 서비스.
 *
 * <p>(QT 본문, 목소리) 조합이 캐시에 있으면 즉시 반환하고, 없으면 본문(한글 절 범위)을 외부 TTS 서버로
 * 생성해 DB에 저장한 뒤 반환한다. 무료 호스팅 콜드스타트/재생성을 그날 1회로 줄인다.
 *
 * <p>외부 HTTP(생성)는 긴 시간이 걸릴 수 있어 클래스 전체를 트랜잭션으로 묶지 않는다.
 * 캐시 조회/저장은 각 리포지토리 호출이 자체 트랜잭션으로 처리한다.
 * 본문만 읽으며 노트·해설·영상·영어는 포함하지 않는다(CLAUDE.md §6 범위).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QtTtsAudioService implements GetQtPassageAudioUseCase {

    private final QtPassageAudioRepository audioRepository;
    private final QtPassageRepository qtPassageRepository;
    private final QtPassageVerseRepository qtPassageVerseRepository;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final TtsClient ttsClient;
    private final TtsProperties ttsProperties;
    private final Clock clock;

    @Override
    public QtPassageAudioResponse getAudio(Long qtPassageId, String voice) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        final String useVoice = (voice == null || voice.isBlank())
                ? ttsProperties.getDefaultVoice() : voice.trim();

        // 1) 캐시 히트면 즉시 반환.
        var cached = audioRepository.findAudio(qtPassageId, useVoice);
        if (cached.isPresent()) {
            return new QtPassageAudioResponse(cached.get().getMimeType(), cached.get().getAudioData());
        }

        // 2) 공개 게이트 — ACTIVE이고 공개일(00:00 KST)이 지난 본문만(존재 은닉 위해 404).
        QtPassage passage = qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
        if (passage.getStatus() != QtPassageStatus.ACTIVE
                || passage.getQtDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        // 3) 본문(한글 절 범위) 텍스트 조합 — display_order 순서.
        String text = composeKoreanText(qtPassageId);
        if (text.isBlank()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        // 4) 외부 TTS 생성(트랜잭션 밖, 느릴 수 있음).
        TtsClient.GeneratedAudio audio = ttsClient.generate(text, useVoice);

        // 5) DB 저장(캐시). 동시 첫 요청 경합으로 unique 위반 시 캐시를 다시 읽어 반환.
        try {
            audioRepository.save(QtPassageAudio.of(
                    qtPassageId, useVoice, audio.mimeType(), audio.data(), LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException race) {
            var now = audioRepository.findAudio(qtPassageId, useVoice);
            if (now.isPresent()) {
                return new QtPassageAudioResponse(now.get().getMimeType(), now.get().getAudioData());
            }
        }
        return new QtPassageAudioResponse(audio.mimeType(), audio.data());
    }

    /** QT 본문 절들의 한글 텍스트를 display_order 순서로 합친다(다른 도메인은 api 포트로). */
    private String composeKoreanText(Long qtPassageId) {
        List<Long> verseIds = qtPassageVerseRepository
                .findByQtPassageIdOrderByDisplayOrderAsc(qtPassageId)
                .stream()
                .map(QtPassageVerse::getBibleVerseId)
                .toList();
        if (verseIds.isEmpty()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }
        Map<Long, String> textById = getBibleVerseUseCase.getVerses(verseIds).stream()
                .collect(Collectors.toMap(BibleVerseResponse::id, BibleVerseResponse::koreanText,
                        (a, b) -> a));
        StringBuilder sb = new StringBuilder();
        for (Long id : verseIds) {
            String t = textById.get(id);
            if (t != null && !t.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t.trim());
            }
        }
        return sb.toString().trim();
    }
}
