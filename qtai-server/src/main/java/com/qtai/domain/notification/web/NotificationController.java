package com.qtai.domain.notification.web;

/**
 * 알림 REST 엔드포인트. base path: /api/v1/notifications
 *
 * 엔드포인트:
 *   GET   /             → 내 알림 목록 (unreadOnly 쿼리 파라미터)
 *   PATCH /{id}/read    → 단건 읽음 처리
 *   PATCH /read-all     → 전체 읽음 처리
 *
 * SendNotificationUseCase는 외부에 노출하지 않음 — 타 도메인이 직접 호출.
 */
// TODO: @RestController, @RequestMapping("/api/v1/notifications"), @RequiredArgsConstructor
public class NotificationController {

    // TODO: ListNotificationUseCase, MarkAsReadUseCase 주입
    // TODO: 3개 엔드포인트 — @AuthenticationPrincipal memberId + ApiResponse
}
