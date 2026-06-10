package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    ListActiveMemberIdsUseCase listActiveMemberIdsUseCase;
    @Mock
    WriteAuditLogUseCase writeAuditLogUseCase;
    @Mock
    NoticePublishStateService noticePublishStateService;
    @Mock
    NoticeNotificationFanoutService noticeNotificationFanoutService;

    NoticeService noticeService;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        noticeService = new NoticeService(
                noticeRepository,
                listActiveMemberIdsUseCase,
                new NoticeAuditWriter(writeAuditLogUseCase),
                new NoticeAuditSnapshotFactory(objectMapper),
                noticePublishStateService,
                noticeNotificationFanoutService
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
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);

        var response = noticeService.updateNotice(1L, new AdminNoticeCommand(
                100L, "수정 공지", "수정 본문", null));

        assertThat(response.title()).isEqualTo("수정 공지");
        assertThat(response.body()).isEqualTo("수정 본문");
        verify(writeAuditLogUseCase).write(captor.capture());
        assertThat(captor.getValue().actionType()).isEqualTo("NOTICE_UPDATE");
    }

    @Test
    void update_rejectsStatusField() {
        assertThatThrownBy(() -> noticeService.updateNotice(1L, new AdminNoticeCommand(
                100L, "수정 공지", "수정 본문", "PUBLISHED")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(noticeRepository, never()).findById(any());
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
        when(listActiveMemberIdsUseCase.listActiveMemberIds()).thenReturn(List.of(10L, 11L));
        when(noticePublishStateService.publish(1L)).thenReturn(new PublishedNotice(
                1L, "공지", "본문", "PUBLISHED",
                LocalDateTime.of(2026, 6, 10, 10, 30), "{\"status\":\"DRAFT\"}"));
        when(noticeNotificationFanoutService.fanout(any(), any())).thenReturn(
                new NoticeNotificationFanoutResult(2, 2, 0));

        var response = noticeService.publishNotice(100L, 1L);

        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 10, 30));
        assertThat(response.notificationResult().requestedCount()).isEqualTo(2);
        assertThat(response.notificationResult().createdCount()).isEqualTo(2);
        assertThat(response.notificationResult().failedCount()).isZero();
        verify(noticeRepository, never()).findById(1L);
    }

    @Test
    void hide_changesDraftToHidden() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        noticeService.hideNotice(100L, 1L);

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.HIDDEN);
    }

    @Test
    void hide_rejectsAlreadyHiddenNotice() {
        Notice notice = persisted(Notice.draft(100L, "공지", "본문"), 1L);
        notice.hide();
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.hideNotice(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void list_rejectsInvalidPageRequest() {
        assertThatThrownBy(() -> noticeService.listAdminNotices(-1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> noticeService.listAdminNotices(0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
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
    void create_auditSnapshotUsesJsonSerializerForControlCharacters() throws Exception {
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);

        noticeService.createNotice(new AdminNoticeCommand(
                100L, "공지\n탭\t문자", "본문", null));

        verify(writeAuditLogUseCase).write(captor.capture());
        assertThat(objectMapper.readTree(captor.getValue().afterJson()).get("title").asText())
                .isEqualTo("공지\n탭\t문자");
    }

    @Test
    void publish_auditSnapshotIncludesNotificationResult() {
        when(listActiveMemberIdsUseCase.listActiveMemberIds()).thenReturn(List.of(10L, 11L));
        when(noticePublishStateService.publish(1L)).thenReturn(new PublishedNotice(
                1L, "공지", "본문", "PUBLISHED",
                LocalDateTime.of(2026, 6, 10, 10, 30), "{\"status\":\"DRAFT\"}"));
        when(noticeNotificationFanoutService.fanout(any(), any())).thenReturn(
                new NoticeNotificationFanoutResult(2, 1, 1));
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);

        noticeService.publishNotice(100L, 1L);

        verify(writeAuditLogUseCase).write(captor.capture());
        assertThat(captor.getValue().afterJson())
                .contains("\"notificationResult\"")
                .contains("\"requestedCount\":2")
                .contains("\"createdCount\":1")
                .contains("\"failedCount\":1");
    }

    @Test
    void create_rejectsHtmlTagCharacters() {
        assertThatThrownBy(() -> noticeService.createNotice(new AdminNoticeCommand(
                100L, "<script>alert(1)</script>", "<img src=x onerror=alert(1)>", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void publish_rejectsInvalidTransition() {
        when(noticePublishStateService.publish(1L))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

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
