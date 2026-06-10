package com.qtai.domain.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 알림 발송 요청 DTO.
 *
 * <p>호출자: 타 도메인 Service (좋아요, 댓글, 신고 결과 등).
 */
public record NotificationSendRequest(
        @NotNull Long memberId,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String body,
        Long noticeId,
        @Size(max = 50) String linkType,
        Long linkId,
        @Size(max = 100) String eventKey
) {}
