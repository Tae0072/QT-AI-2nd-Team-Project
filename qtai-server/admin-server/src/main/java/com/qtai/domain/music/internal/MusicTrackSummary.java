package com.qtai.domain.music.internal;

/**
 * 음원 목록 projection — {@code audio_data} 를 제외한 메타데이터만 조회한다.
 *
 * <p>closed interface projection 이라 Spring Data 가 getter 에 해당하는 컬럼만 SELECT 한다.
 */
public interface MusicTrackSummary {
    Long getId();

    String getTitle();

    MusicCategory getCategory();

    String getMimeType();

    Long getByteSize();

    Integer getDurationSec();

    Integer getSortOrder();
}
