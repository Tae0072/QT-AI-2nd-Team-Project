package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.member.api.ListActiveMemberIdsUseCase;
import com.qtai.domain.notification.api.dto.AdminNoticeCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T01:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    NoticeRepository noticeRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    ListActiveMemberIdsUseCase listActiveMemberIdsUseCase;
    @Mock
    WriteAuditLogUseCase writeAuditLogUseCase;

    NoticeService noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeService(
                noticeRepository,
                notificationRepository,
                listActiveMemberIdsUseCase,
                writeAuditLogUseCase,
                CLOCK
        );
    }

    @Test
    void create_savesDraftNotice() {
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));

        var response = noticeService.createNotice(new AdminNoticeCommand(
                100L, "공지", "본문", null));

        assertThat(response.status()).isEqualTo("DRAFT");
        verify(writeAuditLogUseCase).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void update_allowsDraftOnly() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        var response = noticeService.updateNotice(1L, new AdminNoticeCommand(
                100L, "수정 공지", "수정 본문", null));

        assertThat(response.title()).isEqualTo("수정 공지");
        assertThat(response.body()).isEqualTo("수정 본문");
    }

    @Test
    void update_rejectsPublishedNotice() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        notice.publish(CLOCK);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.updateNotice(1L, new AdminNoticeCommand(
                100L, "수정 공지", "수정 본문", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void publish_changesDraftToPublishedAndCreatesNotifications() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(listActiveMemberIdsUseCase.listActiveMemberIds()).thenReturn(List.of(10L, 11L));
        when(notificationRepository.existsByMemberIdAndEventKey(any(), any())).thenReturn(false);

        var response = noticeService.publishNotice(100L, 1L);

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.PUBLISHED);
        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 10, 30));
        assertThat(response.notificationResult().requestedCount()).isEqualTo(2);
        assertThat(response.notificationResult().createdCount()).isEqualTo(2);
        assertThat(response.notificationResult().failedCount()).isZero();
    }

    @Test
    void hide_changesDraftToHidden() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        noticeService.hideNotice(100L, 1L);

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.HIDDEN);
    }

    @Test
    void create_auditSnapshotDoesNotContainRawBody() {
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);

        noticeService.createNotice(new AdminNoticeCommand(
                100L, "공지", "RAW_BODY_SHOULD_NOT_BE_STORED", null));

        verify(writeAuditLogUseCase).write(captor.capture());
        assertThat(captor.getValue().afterJson()).contains("공지").contains("DRAFT");
        assertThat(captor.getValue().afterJson()).doesNotContain("RAW_BODY_SHOULD_NOT_BE_STORED");
    }

    @Test
    void publish_rejectsInvalidTransition() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        notice.hide();
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.publishNotice(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    private static Notice persisted(Notice notice, Long id) {
        ReflectionTestUtils.setField(notice, "id", id);
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.of(2026, 6, 10, 10, 0));
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.of(2026, 6, 10, 10, 0));
        return notice;
    }
}
