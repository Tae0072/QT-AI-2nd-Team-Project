package com.qtai.domain.qtvideo.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

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
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("사용자 노출 조회는 소프트 삭제(deleted_at) 클립을 제외한다")
    void userCandidateQuery_excludesSoftDeletedClips() {
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(
                null, (short) 46, "https://cdn.example.com/videos/corinthians-full.mp4");
        entityManager.persist(sourceVideo);
        QtVideoClip active = TestEntityFactory.qtVideoClip(
                null, 8L, sourceVideo, "https://cdn.example.com/videos/corinthians-full.mp4",
                QtVideoClipStatus.APPROVED);
        QtVideoClip deleted = TestEntityFactory.qtVideoClip(
                null, 9L, sourceVideo, "https://cdn.example.com/videos/corinthians-full.mp4",
                QtVideoClipStatus.APPROVED);
        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();
        // 소프트 삭제: status는 그대로 APPROVED이고 deleted_at만 채워진다.
        ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.now());
        entityManager.persist(deleted);
        entityManager.flush();
        entityManager.clear();

        List<QtVideoClipStatus> candidates = QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES;
        assertThat(repository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                8L, candidates)).hasSize(1);
        // 삭제되지 않았다면 status=APPROVED라 노출됐겠지만, deleted_at이 있어 제외된다.
        assertThat(repository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                9L, candidates)).isEmpty();
    }
}
