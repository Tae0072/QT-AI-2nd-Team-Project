package com.qtai.domain.notification.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
                NoticeNotificationFanoutResult retryResult = retryChunk(notice, chunk, now, e, "제약 위반");
                createdCount += retryResult.createdCount();
                failedCount += retryResult.failedCount();
            } catch (DataAccessException e) {
                NoticeNotificationFanoutResult retryResult = retryChunk(notice, chunk, now, e, "저장 실패");
                createdCount += retryResult.createdCount();
                failedCount += retryResult.failedCount();
            }
        }
        return new NoticeNotificationFanoutResult(memberIds.size(), createdCount, failedCount);
    }

    private NoticeNotificationFanoutResult retryChunk(
            PublishedNotice notice, List<Long> chunk, LocalDateTime now, DataAccessException exception, String reason) {
        log.warn("공지 알림 청크 {}: noticeId={}, chunkSize={}, errorType={}, errorMessage={}",
                reason, notice.id(), chunk.size(), exception.getClass().getSimpleName(),
                exception.getMostSpecificCause().getMessage());
        return retryIndividually(notice, chunk, now);
    }

    private NoticeNotificationFanoutResult retryIndividually(
            PublishedNotice notice, List<Long> memberIds, LocalDateTime now) {
        long createdCount = 0;
        long failedCount = 0;
        for (Long memberId : memberIds) {
            try {
                createdCount += chunkWriter.writeChunk(notice, Collections.singletonList(memberId), now);
            } catch (DataIntegrityViolationException e) {
                if (chunkWriter.existsNoticeNotification(notice, memberId)) {
                    log.info("공지 알림 중복 skip: noticeId={}, memberId={}", notice.id(), memberId);
                } else {
                    failedCount++;
                    log.warn("공지 알림 단건 저장 제약 위반: noticeId={}, memberId={}, errorMessage={}",
                            notice.id(), memberId, e.getMostSpecificCause().getMessage());
                }
            } catch (DataAccessException e) {
                failedCount++;
                log.warn("공지 알림 단건 저장 실패: noticeId={}, memberId={}, errorType={}, errorMessage={}",
                        notice.id(), memberId, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return new NoticeNotificationFanoutResult(memberIds.size(), createdCount, failedCount);
    }
}
