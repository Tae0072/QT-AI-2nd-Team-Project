package com.qtai.domain.notification.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NoticeAuditSnapshotFactory {

    private final ObjectMapper objectMapper;

    String snapshot(Notice notice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", notice.getId());
        payload.put("title", notice.getTitle());
        payload.put("status", notice.getStatus().name());
        return toJson(payload);
    }

    String snapshot(Notice notice, NoticeNotificationFanoutResult result) {
        return snapshot(notice.getId(), notice.getTitle(), notice.getStatus().name(), result);
    }

    String snapshot(PublishedNotice notice, NoticeNotificationFanoutResult result) {
        return snapshot(notice.id(), notice.title(), notice.status(), result);
    }

    private String snapshot(Long id, String title, String status, NoticeNotificationFanoutResult result) {
        Map<String, Object> notificationResult = new LinkedHashMap<>();
        notificationResult.put("requestedCount", result.requestedCount());
        notificationResult.put("createdCount", result.createdCount());
        notificationResult.put("failedCount", result.failedCount());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("title", title);
        payload.put("status", status);
        payload.put("notificationResult", notificationResult);
        return toJson(payload);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("공지 감사 snapshot 직렬화 실패", exception);
        }
    }
}
