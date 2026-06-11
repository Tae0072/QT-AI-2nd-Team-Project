package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.bible.JpaAuditingConfig;
import com.qtai.common.config.TimeConfig;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

@DataJpaTest
@ContextConfiguration(classes = BibleServiceApplication.class)
@Import({JpaAuditingConfig.class, TimeConfig.class})
class BibleVerseVideoSegmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BibleVerseVideoSegmentRepository repository;

    @Test
    @DisplayName("Finds only non-deleted segments from active source videos")
    void findActiveSourceSegmentsByVerseIds_filtersByActiveSource() {
        SourceVideo activeSource = SourceVideo.active(
                (short) 46,
                "1 Corinthians active",
                SourceVideoStorageProvider.EXTERNAL_URL,
                "https://cdn.example.com/1co-active.mp4",
                new BigDecimal("600.000")
        );
        SourceVideo inactiveSource = SourceVideo.active(
                (short) 47,
                "2 Corinthians inactive",
                SourceVideoStorageProvider.EXTERNAL_URL,
                "https://cdn.example.com/2co-inactive.mp4",
                new BigDecimal("600.000")
        );
        ReflectionTestUtils.setField(inactiveSource, "status", SourceVideoStatus.INACTIVE);

        entityManager.persist(activeSource);
        entityManager.persist(inactiveSource);
        entityManager.persist(TestEntityFactory.bibleVerseVideoSegment(100L, activeSource, "20.000", "30.000"));
        entityManager.persist(TestEntityFactory.bibleVerseVideoSegment(101L, activeSource, "10.000", "20.000"));
        entityManager.persist(TestEntityFactory.bibleVerseVideoSegment(200L, inactiveSource, "10.000", "20.000"));
        entityManager.flush();
        entityManager.clear();

        List<BibleVerseVideoSegment> segments = repository.findActiveSourceSegmentsByVerseIds(
                List.of(100L, 101L, 200L),
                SourceVideoStatus.ACTIVE,
                SourceVideo.ACTIVE_UNIQUE_KEY
        );

        assertEquals(2, segments.size());
        assertEquals(101L, segments.get(0).getBibleVerseId());
        assertEquals(100L, segments.get(1).getBibleVerseId());
        assertEquals("https://cdn.example.com/1co-active.mp4", segments.get(0).getSourceVideo().getVideoUrl());
    }
}
