package com.qtai.domain.notification.internal;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class NoticeNotificationChunkWriter {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int writeChunk(PublishedNotice notice, List<Long> memberIds, LocalDateTime now) {
        Set<Long> uniqueMemberIds = new LinkedHashSet<>(memberIds);
        Map<Long, String> eventKeysByMemberId = uniqueMemberIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        memberId -> "NOTICE:" + notice.id() + ":" + memberId,
                        (left, right) -> left
                ));
        Set<String> existingEventKeys = Set.copyOf(
                notificationRepository.findEventKeysIn(eventKeysByMemberId.values()));
        List<Notification> notifications = eventKeysByMemberId.entrySet().stream()
                .filter(entry -> !existingEventKeys.contains(entry.getValue()))
                .map(entry -> Notification.builder()
                        .memberId(entry.getKey())
                        .type(NotificationType.NOTICE)
                        .title(notice.title())
                        .body(NoticeService.preview(notice.body(), 497))
                        .noticeId(notice.id())
                        .linkType("NOTICE")
                        .linkId(notice.id())
                        .eventKey(entry.getValue())
                        .createdAt(now)
                        .build())
                .toList();
        if (notifications.isEmpty()) {
            return 0;
        }
        notificationRepository.saveAll(notifications);
        notificationRepository.flush();
        return notifications.size();
    }
}
