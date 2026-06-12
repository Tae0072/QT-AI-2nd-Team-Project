package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.bible.JpaAuditingConfig;
import com.qtai.common.config.TimeConfig;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = BibleServiceApplication.class)
@Import({JpaAuditingConfig.class, TimeConfig.class})
class QtVideoClipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QtVideoClipRepository repository;

    @Test
    @DisplayName("existsByQtPassageIdAndStatus checks QT video clip status with real JPA mapping")
    void existsByQtPassageIdAndStatus_checksStatus() {
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(
                null,
                (short) 46,
                "https://cdn.example.com/videos/corinthians-full.mp4"
        );
        entityManager.persist(sourceVideo);
        entityManager.persist(TestEntityFactory.qtVideoClip(
                null,
                6L,
                sourceVideo,
                "https://cdn.example.com/videos/corinthians-full.mp4",
                QtVideoClipStatus.APPROVED
        ));
        entityManager.persist(TestEntityFactory.qtVideoClip(
                null,
                7L,
                sourceVideo,
                "https://cdn.example.com/videos/corinthians-full.mp4",
                QtVideoClipStatus.HIDDEN
        ));
        entityManager.flush();
        entityManager.clear();

        assertTrue(repository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.APPROVED));
        assertFalse(repository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN));
        assertTrue(repository.existsByQtPassageIdAndStatus(7L, QtVideoClipStatus.HIDDEN));
        assertFalse(repository.existsByQtPassageIdAndStatus(999L, QtVideoClipStatus.APPROVED));
    }
}
