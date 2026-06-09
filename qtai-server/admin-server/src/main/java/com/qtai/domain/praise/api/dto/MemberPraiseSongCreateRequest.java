package com.qtai.domain.praise.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 내 찬양 저장 요청 DTO.
 *
 * <p>API 명세서 §4.6.4 기준.
 * <p>praiseSongId OR deviceSongKey 중 하나 이상 필수.
 */
public record MemberPraiseSongCreateRequest(
        Long praiseSongId,
        @Size(max = 200) String deviceSongKey,
        @NotBlank @Size(max = 100) String displayTitle
) {
    /** praiseSongId 또는 deviceSongKey 중 하나 이상 필수. */
    @JsonIgnore
    @AssertTrue(message = "praiseSongId 또는 deviceSongKey 중 하나 이상 필수입니다.")
    public boolean isSourcePresent() {
        return praiseSongId != null || (deviceSongKey != null && !deviceSongKey.isBlank());
    }
}
