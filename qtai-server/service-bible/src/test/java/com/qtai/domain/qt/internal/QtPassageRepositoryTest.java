package com.qtai.domain.qt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.bible.JpaAuditingConfig;
import com.qtai.common.config.TimeConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * {@link QtPassageRepository#findContainingRange} 저장소 슬라이스 테스트.
 *
 * <p>성경 목차에서 선택한 범위를 포함하는 QT 본문 매칭 규칙(같은 권·장 + startVerse..endVerse가
 * 선택 범위를 포함)을 H2에서 고정한다.
 */
@DataJpaTest
@ContextConfiguration(classes = BibleServiceApplication.class)
@Import({JpaAuditingConfig.class, TimeConfig.class})
class QtPassageRepositoryTest {

    @Autowired
    private QtPassageRepository repository;

    private QtPassage seed(short bookId, short chapter, short start, short end) {
        return repository.save(QtPassage.create(
                LocalDate.now().minusDays(1), bookId, chapter, start, end, "제목", "ref"));
    }

    @Test
    @DisplayName("선택 범위를 포함하는 같은 권·장 본문을 반환한다")
    void findContainingRange_포함() {
        QtPassage passage = seed((short) 1, (short) 1, (short) 1, (short) 6);

        List<QtPassage> result =
                repository.findContainingRange((short) 1, (short) 1, (short) 2, (short) 4);

        assertEquals(1, result.size());
        assertEquals(passage.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("범위가 본문 절 범위를 벗어나면 비어 있다")
    void findContainingRange_범위_초과() {
        seed((short) 1, (short) 1, (short) 1, (short) 3);

        assertTrue(
                repository.findContainingRange((short) 1, (short) 1, (short) 1, (short) 5).isEmpty());
    }

    @Test
    @DisplayName("장이 다르면 비어 있다")
    void findContainingRange_다른_장() {
        seed((short) 1, (short) 1, (short) 1, (short) 6);

        assertTrue(
                repository.findContainingRange((short) 1, (short) 2, (short) 1, (short) 1).isEmpty());
    }

    @Test
    @DisplayName("자동게시 대상은 수집 시각이 있는 PENDING 본문만 반환한다")
    void findAutoPublishTargets_requiresCollectedAt() {
        LocalDate cutoff = LocalDate.of(2026, 6, 15);
        QtPassage autoCollected = QtPassage.create(
                cutoff, (short) 1, (short) 1, (short) 1, (short) 3, "auto", "ref");
        autoCollected.scheduleForAutoPublish();
        autoCollected.recordCollected(LocalDateTime.of(2026, 6, 15, 0, 2), null);
        repository.save(autoCollected);

        QtPassage manualPending = QtPassage.create(
                cutoff.minusDays(1), (short) 1, (short) 1, (short) 4, (short) 6, "manual", "ref");
        manualPending.scheduleForAutoPublish();
        repository.save(manualPending);

        List<QtPassage> result =
                repository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, cutoff);

        assertEquals(1, result.size());
        assertEquals(autoCollected.getId(), result.get(0).getId());
    }
}
