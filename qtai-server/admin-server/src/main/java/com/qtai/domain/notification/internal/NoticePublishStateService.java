package com.qtai.domain.notification.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class NoticePublishStateService {

    private final NoticeRepository noticeRepository;
    private final NoticeAuditSnapshotFactory noticeAuditSnapshotFactory;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PublishedNotice publish(Long noticeId) {
        Notice notice = noticeRepository.findByIdForUpdate(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String before = noticeAuditSnapshotFactory.snapshot(notice);
        notice.publish(clock);
        return new PublishedNotice(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getStatus().name(),
                notice.getPublishedAt(),
                before
        );
    }
}
