package com.qtai.domain.notification.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class NoticeNotificationFanoutService {

    private static final int CHUNK_SIZE = 500;

    private final NoticeNotificationChunkWriter chunkWriter;
    private final Clock clock;

    NoticeNotificationFanoutResult fanout(PublishedNotice notice, List<Long> rawMemberIds) {
        List<Long> memberIds = new ArrayList<>(new LinkedHashSet<>(rawMemberIds));
        long createdCount = 0;
        long failedCount = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (int start = 0; start < memberIds.size(); start += CHUNK_SIZE) {
            List<Long> chunk = memberIds.subList(start, Math.min(start + CHUNK_SIZE, memberIds.size()));
            try {
                createdCount += chunkWriter.writeChunk(notice, chunk, now);
            } catch (DataIntegrityViolationException e) {
                log.warn("공지 알림 청크 저장 제약 위반: noticeId={}, chunkSize={}, errorMessage={}",
                        notice.id(), chunk.size(), e.getMostSpecificCause().getMessage());
                NoticeNotificationFanoutResult retryResult = retryIndividually(notice, chunk, now);
                createdCount += retryResult.createdCount();
                failedCount += retryResult.failedCount();
            } catch (RuntimeException e) {
                log.warn("공지 알림 청크 저장 실패: noticeId={}, chunkSize={}, errorType={}, errorMessage={}",
                        notice.id(), chunk.size(), e.getClass().getSimpleName(), e.getMessage());
                NoticeNotificationFanoutResult retryResult = retryIndividually(notice, chunk, now);
                createdCount += retryResult.createdCount();
                failedCount += retryResult.failedCount();
            }
        }
        return new NoticeNotificationFanoutResult(memberIds.size(), createdCount, failedCount);
    }

    private NoticeNotificationFanoutResult retryIndividually(
            PublishedNotice notice, List<Long> memberIds, LocalDateTime now) {
        long createdCount = 0;
        long failedCount = 0;
        for (Long memberId : memberIds) {
            try {
                createdCount += chunkWriter.writeChunk(notice, Collections.singletonList(memberId), now);
            } catch (DataIntegrityViolationException e) {
                failedCount++;
                log.warn("공지 알림 단건 저장 제약 위반: noticeId={}, memberId={}, errorMessage={}",
                        notice.id(), memberId, e.getMostSpecificCause().getMessage());
            } catch (RuntimeException e) {
                failedCount++;
                log.warn("공지 알림 단건 저장 실패: noticeId={}, memberId={}, errorType={}, errorMessage={}",
                        notice.id(), memberId, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return new NoticeNotificationFanoutResult(memberIds.size(), createdCount, failedCount);
    }
}
