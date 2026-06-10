package com.qtai.domain.notification.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.member.api.ListActiveMemberIdsUseCase;
import com.qtai.domain.notification.api.CreateAdminNoticeUseCase;
import com.qtai.domain.notification.api.HideAdminNoticeUseCase;
import com.qtai.domain.notification.api.ListAdminNoticesUseCase;
import com.qtai.domain.notification.api.PublishAdminNoticeUseCase;
import com.qtai.domain.notification.api.UpdateAdminNoticeUseCase;
import com.qtai.domain.notification.api.dto.AdminNoticeCommand;
import com.qtai.domain.notification.api.dto.AdminNoticeDetailResponse;
import com.qtai.domain.notification.api.dto.AdminNoticeListResponse;
import com.qtai.domain.notification.api.dto.AdminNoticePublishResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService implements ListAdminNoticesUseCase, CreateAdminNoticeUseCase, UpdateAdminNoticeUseCase,
        PublishAdminNoticeUseCase, HideAdminNoticeUseCase {

    private static final int BODY_PREVIEW_LENGTH = 80;

    private final NoticeRepository noticeRepository;
    private final NotificationRepository notificationRepository;
    private final ListActiveMemberIdsUseCase listActiveMemberIdsUseCase;
    private final WriteAuditLogUseCase writeAuditLogUseCase;
    private final Clock clock;

    @Override
    public AdminNoticeListResponse listAdminNotices(int page, int size) {
        validatePage(page, size);
        Page<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new AdminNoticeListResponse(
                notices.getContent().stream().map(this::toItem).toList(),
                notices.getNumber(),
                notices.getSize(),
                notices.getTotalElements(),
                notices.getTotalPages(),
                notices.isFirst(),
                notices.isLast()
        );
    }

    @Override
    @Transactional
    public AdminNoticeDetailResponse createNotice(AdminNoticeCommand command) {
        validateCreateStatus(command.status());
        Notice notice = noticeRepository.save(Notice.draft(command.adminUserId(), command.title(), command.body()));
        writeAudit(command.adminUserId(), "NOTICE_CREATE", notice, null, snapshot(notice));
        return toDetail(notice);
    }

    @Override
    @Transactional
    public AdminNoticeDetailResponse updateNotice(Long noticeId, AdminNoticeCommand command) {
        Notice notice = findNotice(noticeId);
        String before = snapshot(notice);
        notice.updateDraft(command.title(), command.body());
        writeAudit(command.adminUserId(), "NOTICE_UPDATE", notice, before, snapshot(notice));
        return toDetail(notice);
    }

    @Override
    @Transactional
    public AdminNoticePublishResponse publishNotice(Long adminUserId, Long noticeId) {
        Notice notice = findNotice(noticeId);
        String before = snapshot(notice);
        notice.publish(clock);
        NotificationStats stats = createNoticeNotifications(notice);
        String after = snapshot(notice, stats);
        writeAudit(adminUserId, "NOTICE_PUBLISH", notice, before, after);
        if (stats.failedCount() > 0) {
            log.warn("공지 알림 일부 생성 실패: noticeId={}, requestedCount={}, createdCount={}, failedCount={}",
                    notice.getId(), stats.requestedCount(), stats.createdCount(), stats.failedCount());
        }
        return new AdminNoticePublishResponse(
                notice.getId(),
                notice.getStatus().name(),
                notice.getPublishedAt(),
                new AdminNoticePublishResponse.NotificationResult(
                        stats.requestedCount(), stats.createdCount(), stats.failedCount())
        );
    }

    @Override
    @Transactional
    public void hideNotice(Long adminUserId, Long noticeId) {
        Notice notice = findNotice(noticeId);
        String before = snapshot(notice);
        notice.hide();
        writeAudit(adminUserId, "NOTICE_HIDE", notice, before, snapshot(notice));
    }

    private NotificationStats createNoticeNotifications(Notice notice) {
        List<Long> memberIds = listActiveMemberIdsUseCase.listActiveMemberIds();
        long createdCount = 0;
        long failedCount = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long memberId : memberIds) {
            String eventKey = "NOTICE:" + notice.getId() + ":" + memberId;
            try {
                if (notificationRepository.existsByMemberIdAndEventKey(memberId, eventKey)) {
                    continue;
                }
                notificationRepository.save(Notification.builder()
                        .memberId(memberId)
                        .type(NotificationType.NOTICE)
                        .title(notice.getTitle())
                        .body(preview(notice.getBody(), 500))
                        .noticeId(notice.getId())
                        .linkType("NOTICE")
                        .linkId(notice.getId())
                        .eventKey(eventKey)
                        .createdAt(now)
                        .build());
                createdCount++;
            } catch (DataIntegrityViolationException e) {
                failedCount++;
                log.warn("공지 알림 중복 또는 제약 위반: noticeId={}, memberId={}, eventKey={}",
                        notice.getId(), memberId, eventKey);
            } catch (RuntimeException e) {
                failedCount++;
                log.warn("공지 알림 생성 실패: noticeId={}, memberId={}, errorType={}, errorMessage={}",
                        notice.getId(), memberId, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return new NotificationStats(memberIds.size(), createdCount, failedCount);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AdminNoticeDetailResponse toDetail(Notice notice) {
        return new AdminNoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getStatus().name(),
                notice.getPublishedAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private AdminNoticeListResponse.Item toItem(Notice notice) {
        return new AdminNoticeListResponse.Item(
                notice.getId(),
                notice.getTitle(),
                preview(notice.getBody(), BODY_PREVIEW_LENGTH),
                notice.getStatus().name(),
                notice.getPublishedAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private void writeAudit(Long adminUserId, String actionType, Notice notice, String beforeJson, String afterJson) {
        writeAuditLogUseCase.write(new AuditLogWriteRequest(
                adminUserId,
                "ADMIN",
                adminUserId,
                "ADMIN:" + adminUserId,
                actionType,
                "NOTICE",
                notice.getId(),
                beforeJson,
                afterJson
        ));
    }

    private static void validateCreateStatus(String status) {
        if (status == null || status.isBlank() || NoticeStatus.DRAFT.name().equals(status)) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "공지 생성 상태는 DRAFT만 허용됩니다.");
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100이어야 합니다.");
        }
    }

    private static String preview(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private static String snapshot(Notice notice) {
        return "{\"id\":" + notice.getId()
                + ",\"title\":\"" + escapeJson(notice.getTitle())
                + "\",\"status\":\"" + notice.getStatus().name() + "\"}";
    }

    private static String snapshot(Notice notice, NotificationStats stats) {
        return "{\"id\":" + notice.getId()
                + ",\"title\":\"" + escapeJson(notice.getTitle())
                + "\",\"status\":\"" + notice.getStatus().name()
                + "\",\"notificationResult\":{\"requestedCount\":" + stats.requestedCount()
                + ",\"createdCount\":" + stats.createdCount()
                + ",\"failedCount\":" + stats.failedCount() + "}}";
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record NotificationStats(long requestedCount, long createdCount, long failedCount) {
    }
}
