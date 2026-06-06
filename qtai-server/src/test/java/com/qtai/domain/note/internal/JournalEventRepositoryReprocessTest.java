package com.qtai.domain.note.internal;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * journal_events 재처리 대상(due) 조회 규칙 + previous_qt_passage_id 영속 검증(P1-10).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class JournalEventRepositoryReprocessTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);
    private static final int MAX_RETRY = 3;

    @Autowired
    private JournalEventRepository repository;

    @Autowired
    private TestEntityManager testEm;

    private JournalEvent persist(LocalDateTime occurredAt, Long previousQtPassageId) {
        JournalEvent event = JournalEvent.pending(new JournalChangedEvent(
                UUID.randomUUID(), 10L, 99L, 100L, previousQtPassageId,
                JournalEventType.JOURNAL_UPDATED, NoteStatus.DRAFT, NoteStatus.SAVED, occurredAt));
        return event;
    }

    @Test
    @DisplayName("due 조회는 PENDING과 백오프 도래·상한미만 FAILED만, occurredAt 오름차순으로 반환한다")
    void findDueForReprocess_selectsOnlyDue_inOrder() {
        JournalEvent pendingOld = persist(NOW.minusMinutes(10), null);
        JournalEvent failedDue = persist(NOW.minusMinutes(7), null);
        failedDue.markFailedForRetry("e", NOW.minusMinutes(8), NOW.minusMinutes(1)); // retry=1, due
        JournalEvent pendingNew = persist(NOW.minusMinutes(5), null);

        JournalEvent processed = persist(NOW.minusMinutes(9), null);
        processed.markProcessed(NOW.minusMinutes(8));
        JournalEvent failedFuture = persist(NOW.minusMinutes(6), null);
        failedFuture.markFailedForRetry("e", NOW.minusMinutes(6), NOW.plusHours(1)); // 아직 미도래
        JournalEvent failedOverCap = persist(NOW.minusMinutes(4), null);
        failedOverCap.markFailedForRetry("e1", NOW.minusMinutes(30), NOW.minusMinutes(20));
        failedOverCap.markFailedForRetry("e2", NOW.minusMinutes(20), NOW.minusMinutes(10));
        failedOverCap.markFailedForRetry("e3", NOW.minusMinutes(10), NOW.minusMinutes(1)); // retry=3 == MAX

        for (JournalEvent e : List.of(pendingOld, failedDue, pendingNew, processed, failedFuture, failedOverCap)) {
            testEm.persist(e);
        }
        testEm.flush();

        List<Long> due = repository.findDueForReprocess(NOW, MAX_RETRY, PageRequest.of(0, 50));

        assertThat(due).containsExactly(pendingOld.getId(), failedDue.getId(), pendingNew.getId());
    }

    @Test
    @DisplayName("previous_qt_passage_id가 영속·복원된다")
    void previousQtPassageId_roundTrips() {
        JournalEvent event = persist(NOW.minusMinutes(1), 77L);
        Long id = testEm.persistAndFlush(event).getId();
        testEm.clear();

        JournalEvent reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getPreviousQtPassageId()).isEqualTo(77L);
    }
}
