package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageCommand;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;
import com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminQtPassageServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T01:30:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private QtPassageRepository qtPassageRepository;

    @Mock
    private WriteAuditLogUseCase auditLogUseCase;

    @Mock
    private TodayQtCacheEvictor todayQtCacheEvictor;

    @Mock
    private AdminQtVideoAutoPreparer autoPreparer;

    private AdminQtPassageService service;

    @BeforeEach
    void setUp() {
        service = new AdminQtPassageService(
                qtPassageRepository,
                auditLogUseCase,
                new ObjectMapper().findAndRegisterModules(),
                CLOCK,
                todayQtCacheEvictor,
                autoPreparer
        );
    }

    @Test
    @DisplayName("list returns paged admin responses")
    void list_returnsPagedAdminResponses() {
        QtPassage passage = passage(10L, LocalDate.of(2026, 6, 11));
        when(qtPassageRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(passage), PageRequest.of(0, 20), 1));

        AdminQtPassageListResponse response = service.list(
                new ListAdminQtPassagesQuery("pending_review", null, null, "admin", 0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(10L);
        assertThat(response.content().get(0).status()).isEqualTo("pending_review");
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list rejects invalid status query")
    void list_rejectsInvalidStatusQuery() {
        assertThatThrownBy(() -> service.list(new ListAdminQtPassagesQuery("unknown", null, null, null, 0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("create saves pending_review and writes audit log")
    void create_savesPendingReviewAndWritesAuditLog() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 11));
        when(qtPassageRepository.existsByQtDate(command.qtDate())).thenReturn(false);
        when(qtPassageRepository.save(any(QtPassage.class))).thenAnswer(invocation -> {
            QtPassage passage = invocation.getArgument(0);
            ReflectionTestUtils.setField(passage, "id", 10L);
            return passage;
        });

        AdminQtPassageResponse response = service.create(command);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("pending_review");
        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("QT_PASSAGE_CREATE");
        assertThat(auditCaptor.getValue().targetType()).isEqualTo("QT_PASSAGE");
        assertThat(auditCaptor.getValue().afterJson()).contains("\"status\":\"pending_review\"");
    }

    @Test
    @DisplayName("create rejects duplicate QT date")
    void create_rejectsDuplicateQtDate() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 11));
        when(qtPassageRepository.existsByQtDate(command.qtDate())).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("create accepts a cross-chapter range whose ending verse is lower")
    void create_acceptsCrossChapterRange() {
        AdminQtPassageCommand command = command(
                LocalDate.of(2026, 6, 15), (short) 9, (short) 10, (short) 20, (short) 5);
        when(qtPassageRepository.existsByQtDate(command.qtDate())).thenReturn(false);
        when(qtPassageRepository.save(any(QtPassage.class))).thenAnswer(invocation -> {
            QtPassage passage = invocation.getArgument(0);
            ReflectionTestUtils.setField(passage, "id", 30L);
            return passage;
        });

        AdminQtPassageResponse response = service.create(command);

        assertThat(response.chapter()).isEqualTo((short) 9);
        assertThat(response.endChapter()).isEqualTo((short) 10);
        assertThat(response.startVerse()).isEqualTo((short) 20);
        assertThat(response.endVerse()).isEqualTo((short) 5);
    }

    @Test
    @DisplayName("create rejects a cross-chapter range whose ending chapter is earlier")
    void create_rejectsDescendingChapterRange() {
        AdminQtPassageCommand command = command(
                LocalDate.of(2026, 6, 15), (short) 10, (short) 9, (short) 1, (short) 20);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("종료 장")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("create rejects reversed verses when the range stays in one chapter")
    void create_rejectsDescendingVerseRangeWithinSameChapter() {
        AdminQtPassageCommand command = command(
                LocalDate.of(2026, 6, 15), (short) 9, (short) 9, (short) 20, (short) 5);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("같은 장")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("publish updates status, writes audit log, and evicts cache after commit")
    void publish_updatesStatusAndWritesAuditLog() {
        QtPassage passage = passage(20L, LocalDate.of(2026, 6, 12));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage));

        AdminQtPassageResponse response = service.publish(3L, 20L);

        assertThat(response.status()).isEqualTo("active");
        assertThat(response.publishedAt()).isNotNull();
        assertThat(response.hiddenAt()).isNull();
        verify(auditLogUseCase).write(any(AuditLogWriteRequest.class));
        verify(todayQtCacheEvictor).evictAfterCommit();
    }

    @Test
    @DisplayName("hide updates status, writes audit log, and evicts cache after commit")
    void hide_updatesStatusAndWritesAuditLog() {
        QtPassage passage = passage(20L, LocalDate.of(2026, 6, 12));
        passage.publish(LocalDateTime.now(CLOCK));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage));

        AdminQtPassageResponse response = service.hide(3L, 20L);

        assertThat(response.status()).isEqualTo("hidden");
        assertThat(response.hiddenAt()).isNotNull();
        verify(auditLogUseCase).write(any(AuditLogWriteRequest.class));
        verify(todayQtCacheEvictor).evictAfterCommit();
    }

    @Test
    @DisplayName("update writes audit log and evicts cache after commit")
    void update_evictsTodayQtCacheAfterCommit() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 13));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 12))));
        when(qtPassageRepository.existsByQtDateAndIdNot(eq(command.qtDate()), eq(20L))).thenReturn(false);

        service.update(20L, command);

        verify(auditLogUseCase).write(any(AuditLogWriteRequest.class));
        verify(todayQtCacheEvictor).evictAfterCommit();
    }

    @Test
    @DisplayName("publish rejects invalid status transition")
    void publish_rejectsAlreadyActivePassage() {
        QtPassage passage = passage(20L, LocalDate.of(2026, 6, 12));
        passage.publish(LocalDateTime.now(CLOCK));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage));

        assertThatThrownBy(() -> service.publish(3L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("hide rejects invalid status transition")
    void hide_rejectsPendingReviewPassage() {
        QtPassage passage = passage(20L, LocalDate.of(2026, 6, 12));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage));

        assertThatThrownBy(() -> service.hide(3L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("publish rejects missing QT passage")
    void publish_rejectsMissingQtPassage() {
        when(qtPassageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(3L, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("update rejects duplicate QT date from another passage")
    void update_rejectsDuplicateDateFromAnotherPassage() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 13));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 12))));
        when(qtPassageRepository.existsByQtDateAndIdNot(eq(command.qtDate()), eq(20L))).thenReturn(true);

        assertThatThrownBy(() -> service.update(20L, command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("update returns not found before duplicate date check")
    void update_rejectsMissingQtPassageBeforeDuplicateDateCheck() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 13));
        when(qtPassageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("create — 미공개 등록이므로 autoPreparer를 prepareClip=false로 호출")
    void create_triggersAutoPrepareWithoutClip() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 11));
        when(qtPassageRepository.existsByQtDate(command.qtDate())).thenReturn(false);
        when(qtPassageRepository.save(any(QtPassage.class))).thenAnswer(invocation -> {
            QtPassage passage = invocation.getArgument(0);
            ReflectionTestUtils.setField(passage, "id", 10L);
            return passage;
        });

        service.create(command);

        verify(autoPreparer).syncAfterCommit(eq(3L), eq(10L), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    @DisplayName("update — 공개 본문 반영이므로 autoPreparer를 prepareClip=true로 호출")
    void update_triggersAutoPrepareWithClip() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 13));
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 12))));
        when(qtPassageRepository.existsByQtDateAndIdNot(eq(command.qtDate()), eq(20L))).thenReturn(false);

        service.update(20L, command);

        verify(autoPreparer).syncAfterCommit(eq(3L), eq(20L), any(), any(), any(), any(), any(), eq(true));
    }

    @Test
    @DisplayName("publish — 게시 시 autoPreparer를 prepareClip=true로 호출")
    void publish_triggersAutoPrepareWithClip() {
        when(qtPassageRepository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 12))));

        service.publish(3L, 20L);

        verify(autoPreparer).syncAfterCommit(eq(3L), eq(20L), any(), any(), any(), any(), any(), eq(true));
    }

    private static AdminQtPassageCommand command(LocalDate qtDate) {
        return command(qtDate, (short) 23, (short) 23, (short) 1, (short) 6);
    }

    private static AdminQtPassageCommand command(
            LocalDate qtDate,
            short chapter,
            short endChapter,
            short startVerse,
            short endVerse
    ) {
        return new AdminQtPassageCommand(
                3L,
                qtDate,
                (short) 19,
                chapter,
                endChapter,
                startVerse,
                endVerse,
                "Admin QT",
                "Ps 23:1-6"
        );
    }

    private static QtPassage passage(Long id, LocalDate qtDate) {
        QtPassage passage = QtPassage.create(
                qtDate,
                (short) 19,
                (short) 23,
                (short) 1,
                (short) 6,
                "Admin QT",
                "Ps 23:1-6"
        );
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }
}
