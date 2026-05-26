package com.qtai.domain.notification.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.notification.api.MarkAsReadUseCase;
import com.qtai.domain.notification.api.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 REST 엔드포인트.
 *
 * <p>API 명세서 §4.6.3 기준.
 * <p>SendNotificationUseCase 는 컨트롤러에서 호출하지 않음 — 도메인이 직접 호출.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ListNotificationUseCase listNotificationUseCase;
    private final MarkAsReadUseCase markAsReadUseCase;

    /**
     * GET /api/v1/notifications?unreadOnly=true&amp;page=0&amp;size=20
     *
     * <p>내 알림 목록 조회. unreadOnly=true 이면 미읽음만 반환.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> listMy(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "false")
            Boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> page = listNotificationUseCase.listMy(memberId, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * PATCH /api/v1/notifications/{notificationId}/read
     *
     * <p>개별 읽음 처리 → 204 No Content.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long notificationId) {
        markAsReadUseCase.markAsRead(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/v1/notifications/read-all — 전체 읽음 처리. */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Long memberId) {
        markAsReadUseCase.markAllAsRead(memberId);
        return ResponseEntity.noContent().build();
    }
}
