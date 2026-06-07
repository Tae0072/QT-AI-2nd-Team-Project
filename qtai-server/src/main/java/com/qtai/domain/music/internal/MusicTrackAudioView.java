package com.qtai.domain.music.internal;

/**
 * 음원 스트리밍 projection — 바이트 + mime + 크기만 조회한다.
 */
public interface MusicTrackAudioView {
    byte[] getAudioData();

    String getMimeType();

    Long getByteSize();
}
