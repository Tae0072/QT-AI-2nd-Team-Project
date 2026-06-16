package com.qtai.domain.qtvideo.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BibleVerseVideoSegmentRepository extends JpaRepository<BibleVerseVideoSegment, Long> {

    List<BibleVerseVideoSegment> findBySourceVideo_IdOrderByStartTimeSecAscIdAsc(Long sourceVideoId);

    long deleteBySourceVideo_Id(Long sourceVideoId);

    @Query("""
            select segment
              from BibleVerseVideoSegment segment
              join fetch segment.sourceVideo sourceVideo
             where segment.bibleVerseId in :verseIds
               and sourceVideo.status = :sourceStatus
               and sourceVideo.activeUniqueKey = :activeUniqueKey
             order by sourceVideo.id asc, segment.startTimeSec asc
            """)
    List<BibleVerseVideoSegment> findActiveSourceSegmentsByVerseIds(
            @Param("verseIds") Collection<Long> verseIds,
            @Param("sourceStatus") SourceVideoStatus sourceStatus,
            @Param("activeUniqueKey") String activeUniqueKey
    );
}
