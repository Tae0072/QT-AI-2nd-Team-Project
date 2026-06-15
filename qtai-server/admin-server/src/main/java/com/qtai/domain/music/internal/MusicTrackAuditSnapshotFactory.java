package com.qtai.domain.music.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MusicTrackAuditSnapshotFactory {

    private final ObjectMapper objectMapper;

    String snapshot(MusicTrack track) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", track.getId());
        payload.put("title", track.getTitle());
        payload.put("category", track.getCategory().name());
        payload.put("mimeType", track.getMimeType());
        payload.put("byteSize", track.getByteSize());
        payload.put("durationSec", track.getDurationSec());
        payload.put("sortOrder", track.getSortOrder());
        payload.put("licenseNote", track.getLicenseNote());
        payload.put("status", MusicTrackStatus.fromEnabled(track.getEnabled()).name());
        return toJson(payload);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("배경음악 감사 snapshot 직렬화 실패", exception);
        }
    }
}
