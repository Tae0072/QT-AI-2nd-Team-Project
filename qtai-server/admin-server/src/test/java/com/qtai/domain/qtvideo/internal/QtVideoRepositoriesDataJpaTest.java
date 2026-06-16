package com.qtai.domain.qtvideo.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.qtai.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * qtvideo 리포지토리 슬라이스 테스트.
 *
 * <p>규칙에 따라 테스트는 H2(MySQL 모드)에서 수행한다(CLAUDE.md §1: 운영 MySQL / 테스트 H2).
 * 소프트 삭제(deleted_at) 필터·cascade 적재·재등록 유니크 동작을 DB 레벨에서 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class QtVideoRepositoriesDataJpaTest {

    @Autowired
    private SourceVideoRepository sourceVideoRepository;

    @Autowired
    private QtVideoClipRepository clipRepository;

    @Autowired
    private BibleVerseVideoSegmentRepository segmentRepository;

    @Autowired
    private TestEntityManager em;

    private SourceVideo persistActiveSource(short bibleBookId, String videoUrl) {
        return em.persistAndFlush(
                SourceVideo.active(bibleBookId, "title", videoUrl, new BigDecimal("100.000")));
    }

    private BibleVerseVideoSegment persistSegment(SourceVideo source, long verseId, String from, String to) {
        return em.persistAndFlush(
                BibleVerseVideoSegment.create(verseId, source, new BigDecimal(from), new BigDecimal(to)));
    }

    private QtVideoClip persistClip(SourceVideo source, long qtPassageId) {
        return em.persistAndFlush(QtVideoClip.approvedSingleCut(
                qtPassageId, "QT video", source, source.getVideoUrl(),
                new BigDecimal("0.000"), new BigDecimal("10.000"), LocalDateTime.now()));
    }

    @Test
    void findByDeletedAtIsNullExcludesSoftDeletedSources() {
        SourceVideo active = persistActiveSource((short) 46, "u-active");
        SourceVideo removed = persistActiveSource((short) 47, "u-removed");
        removed.softDelete(LocalDateTime.now());
        em.persistAndFlush(removed);

        assertThat(sourceVideoRepository.findByDeletedAtIsNull(PageRequest.of(0, 20)).getContent())
                .extracting(SourceVideo::getId)
                .containsExactly(active.getId());
    }

    @Test
    void cascadeFindersReturnOnlyNonDeletedChildren() {
        SourceVideo source = persistActiveSource((short) 46, "u1");
        BibleVerseVideoSegment segment = persistSegment(source, 1001L, "0.000", "10.000");
        QtVideoClip clip = persistClip(source, 10L);

        assertThat(clipRepository.findBySourceVideo_IdAndDeletedAtIsNull(source.getId())).hasSize(1);
        assertThat(segmentRepository
                .findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(source.getId())).hasSize(1);

        segment.softDelete(LocalDateTime.now());
        clip.softDelete(LocalDateTime.now());
        em.persistAndFlush(segment);
        em.persistAndFlush(clip);

        assertThat(clipRepository.findBySourceVideo_IdAndDeletedAtIsNull(source.getId())).isEmpty();
        assertThat(segmentRepository
                .findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(source.getId())).isEmpty();
    }

    @Test
    void findActiveSourceSegmentsByVerseIdsExcludesDeletedSegment() {
        SourceVideo source = persistActiveSource((short) 46, "u1");
        BibleVerseVideoSegment segment = persistSegment(source, 2001L, "0.000", "10.000");

        assertThat(segmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(2001L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY)).hasSize(1);

        segment.softDelete(LocalDateTime.now());
        em.persistAndFlush(segment);

        assertThat(segmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(2001L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY)).isEmpty();
    }

    @Test
    void deleteBySourceVideoIdHardDeletesSegments() {
        SourceVideo source = persistActiveSource((short) 46, "u1");
        persistSegment(source, 3001L, "0.000", "10.000");
        persistSegment(source, 3002L, "10.000", "20.000");

        long deleted = segmentRepository.deleteBySourceVideo_Id(source.getId());
        em.flush();
        em.clear();

        assertThat(deleted).isEqualTo(2L);
        assertThat(segmentRepository
                .findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(source.getId())).isEmpty();
    }

    @Test
    void softDeletedSourceFreesActiveUniqueKeyForReRegistration() {
        SourceVideo first = persistActiveSource((short) 46, "u1");
        first.softDelete(LocalDateTime.now());
        em.persistAndFlush(first);

        // 같은 성경권으로 새 활성 원본 등록 — uk(bible_book_id, active_unique_key) 충돌이 없어야 한다.
        SourceVideo second = persistActiveSource((short) 46, "u2");
        em.flush();

        assertThat(sourceVideoRepository
                .findByBibleBookIdAndActiveUniqueKey((short) 46, SourceVideo.ACTIVE_UNIQUE_KEY))
                .get()
                .extracting(SourceVideo::getId)
                .isEqualTo(second.getId());
    }
}
