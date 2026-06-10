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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService implements ListAdminNoticesUseCase, CreateAdminNoticeUseCase, UpdateAdminNoticeUseCase,
        PublishAdminNoticeUseCase, HideAdminNoticeUseCase {

    private static final int BODY_PREVIEW_LENGTH = 80;

    private final NoticeRepository noticeRepository;
    private final ListActiveMemberIdsUseCase listActiveMemberIdsUseCase;
    private final WriteAuditLogUseCase writeAuditLogUseCase;
    private final NoticePublishStateService noticePublishStateService;
    private final NoticeNotificationFanoutService noticeNotificationFanoutService;

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AdminNoticePublishResponse publishNotice(Long adminUserId, Long noticeId) {
        PublishedNotice publishedNotice = noticePublishStateService.publish(noticeId);
        NoticeNotificationFanoutResult result = noticeNotificationFanoutService.fanout(
                publishedNotice, listActiveMemberIdsUseCase.listActiveMemberIds());
        writePublishAudit(adminUserId, publishedNotice, result);
        if (result.failedCount() > 0) {
            log.warn("공지 알림 일부 생성 실패: noticeId={}, requestedCount={}, createdCount={}, failedCount={}",
                    publishedNotice.id(), result.requestedCount(), result.createdCount(), result.failedCount());
        }
        return new AdminNoticePublishResponse(
                publishedNotice.id(),
                publishedNotice.status(),
                publishedNotice.publishedAt(),
                new AdminNoticePublishResponse.NotificationResult(
                        result.requestedCount(), result.createdCount(), result.failedCount())
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

    private void writePublishAudit(Long adminUserId, PublishedNotice publishedNotice,
                                   NoticeNotificationFanoutResult result) {
        Notice notice = findNotice(publishedNotice.id());
        writeAudit(adminUserId, "NOTICE_PUBLISH", notice, publishedNotice.beforeJson(), snapshot(notice, result));
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

    static String preview(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    static String snapshot(Notice notice) {
        return "{\"id\":" + notice.getId()
                + ",\"title\":\"" + escapeJson(notice.getTitle())
                + "\",\"status\":\"" + notice.getStatus().name() + "\"}";
    }

    private static String snapshot(Notice notice, NoticeNotificationFanoutResult result) {
        return "{\"id\":" + notice.getId()
                + ",\"title\":\"" + escapeJson(notice.getTitle())
                + "\",\"status\":\"" + notice.getStatus().name()
                + "\",\"notificationResult\":{\"requestedCount\":" + result.requestedCount()
                + ",\"createdCount\":" + result.createdCount()
                + ",\"failedCount\":" + result.failedCount() + "}}";
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
