package com.qtai.domain.music.internal;

import java.time.LocalDateTime;

/**
 * 관리자 배경음악 목록 projection.
 *
 * <p>{@code audio_data}를 제외해 관리자 목록 조회가 음원 바이트를 읽지 않게 한다.
 */
public interface AdminMusicTrackSummary {

    Long getId();

    String getTitle();

    MusicCategory getCategory();

    String getMimeType();

    Long getByteSize();

    Integer getDurationSec();

    Integer getSortOrder();

    Boolean getEnabled();

    String getLicenseNote();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
