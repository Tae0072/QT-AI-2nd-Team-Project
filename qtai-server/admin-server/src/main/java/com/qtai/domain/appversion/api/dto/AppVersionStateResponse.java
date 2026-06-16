package com.qtai.domain.appversion.api.dto;

import java.time.LocalDateTime;

/**
 * 앱 버전 상태 응답 DTO.
 *
 * <p>도메인 경계: api/dto는 internal을 import하지 않으므로 updateMode는 String.
 */
public record AppVersionStateResponse(
        String contentVersion,
        String appVersion,
        String minSupportedVersion,
        String updateMode,
        String updateMessage,
        LocalDateTime updatedAt
) {
}
