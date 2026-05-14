package com.qtai.bff.api;

import com.qtai.bff.client.AiClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 마이 대시보드.
 *
 * <p>경로: GET /api/v1/me/dashboard (인증 필수)
 *
 * <p>TODO(강태오):
 * - Bible Service /api/v1/journals?page=0&size=5 호출 → 최근 묵상
 * - AI Service /ai/sessions?status=IN_PROGRESS → 진행 중 세션
 * - 카운트 (연속 묵상 일수, 이번 주 묵상 수)
 */
@RestController
public class DashboardController {

    private final AiClient aiClient;

    public DashboardController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/api/v1/me/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal Jwt jwt,
                                         @RequestHeader("Authorization") String bearer) {
        Map<?, ?> inProgress = aiClient.listInProgressSessions(bearer);
        return Map.of(
                "userId", jwt.getSubject(),
                "inProgressAiSessions", inProgress,
                "streakDays", 0,         // TODO: 연속 묵상 카운트
                "recentJournals", java.util.List.of()  // TODO: Bible Service 호출
        );
    }
}
