package com.qtai.domain.qt.internal;

/** 음성 캐시 projection — 바이트/MIME/크기만 조회한다. */
public interface QtPassageAudioView {
    byte[] getAudioData();

    String getMimeType();

    Long getByteSize();
}
