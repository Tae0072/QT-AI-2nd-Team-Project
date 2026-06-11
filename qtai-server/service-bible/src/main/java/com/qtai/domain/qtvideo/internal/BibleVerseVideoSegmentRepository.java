package com.qtai.domain.qtvideo.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface BibleVerseVideoSegmentRepository extends JpaRepository<BibleVerseVideoSegment, Long> {

    @Query("""
            select segment
              from BibleVerseVideoSegment segment
              join fetch segment.sourceVideo sourceVideo
             where segment.deletedAt is null
               and segment.bibleVerseId in :verseIds
               and sourceVideo.deletedAt is null
               and sourceVideo.status = :sourceStatus
               and sourceVideo.activeUniqueKey = :activeUniqueKey
             order by sourceVideo.id asc, segment.startTimeSec asc
            """)
    List<BibleVerseVideoSegment> findActiveSourceSegmentsByVerseIds(
            @Param("verseIds") Collection<Long> verseIds,
            @Param("sourceStatus") SourceVideoStatus sourceStatus,
            @Param("activeUniqueKey") String activeUniqueKey);
}
