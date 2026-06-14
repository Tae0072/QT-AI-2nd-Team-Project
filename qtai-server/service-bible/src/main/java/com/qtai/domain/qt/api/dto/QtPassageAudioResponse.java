package com.qtai.domain.qt.api.dto;

/**
 * QT 본문 TTS 음성 응답 — 바이트와 MIME 타입.
 *
 * @param mimeType 예: audio/mpeg
 * @param data     음성 바이트(mp3 등)
 */
public record QtPassageAudioResponse(String mimeType, byte[] data) {
}
