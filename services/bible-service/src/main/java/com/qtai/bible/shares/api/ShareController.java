package com.qtai.bible.shares.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 익명 나눔 API. apis/bible/openapi.yaml 의 shares 태그.
 *
 * <p>경로 (DECISIONS.md §3):
 * - GET    /api/v1/shares                       — 익명 나눔 피드 (비로그인 허용)
 * - POST   /api/v1/journals/{id}/share          — 공개
 * - DELETE /api/v1/journals/{id}/share          — 비공개 전환
 * - POST/DELETE /api/v1/shares/{shareId}/likes
 * - GET/POST   /api/v1/shares/{shareId}/comments
 * - POST   /api/v1/shares/{shareId}/reports
 *
 * <p>MVP: 전체 공개/비공개만 (세분화 X), 좋아요/댓글/신고 포함, 팔로우 제외.
 *
 * <p>TODO(이지윤·이승욱): 엔티티(JournalShare, ShareLike, ShareComment, ShareReport), 신고 운영 큐.
 */
@RestController
public class ShareController {

    @GetMapping("/api/v1/shares")
    public Map<String, Object> feed(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        // TODO: 페이지 단위 익명 피드 + 좋아요/댓글 카운트 조인 (도메인 내 JOIN OK)
        return Map.of("items", List.of(), "page", page, "size", size, "totalElements", 0);
    }

    @PostMapping("/api/v1/journals/{id}/share")
    public ResponseEntity<Map<String, Object>> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        // TODO: shareId 생성, 공개로 마킹
        return ResponseEntity.ok(Map.of("shareId", "TODO", "visibility", "PUBLIC"));
    }

    @DeleteMapping("/api/v1/journals/{id}/share")
    public ResponseEntity<Void> unpublish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/shares/{shareId}/likes")
    public ResponseEntity<Void> like(@AuthenticationPrincipal Jwt jwt, @PathVariable Long shareId) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/shares/{shareId}/likes")
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal Jwt jwt, @PathVariable Long shareId) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/shares/{shareId}/comments")
    public Map<String, Object> comments(@PathVariable Long shareId) {
        return Map.of("items", List.of());
    }

    @PostMapping("/api/v1/shares/{shareId}/comments")
    public ResponseEntity<Map<String, Object>> postComment(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long shareId,
                                                           @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("commentId", "TODO"));
    }

    @PostMapping("/api/v1/shares/{shareId}/reports")
    public ResponseEntity<Void> report(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long shareId,
                                       @RequestBody Map<String, String> body) {
        // TODO: 신고 카운트 누적 + 임계치 초과 시 자동 비공개 처리 후보
        return ResponseEntity.noContent().build();
    }
}
