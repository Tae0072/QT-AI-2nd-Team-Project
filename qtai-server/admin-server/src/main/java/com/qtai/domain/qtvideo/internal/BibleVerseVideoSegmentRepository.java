package com.qtai.domain.qtvideo.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BibleVerseVideoSegmentRepository extends JpaRepository<BibleVerseVideoSegment, Long> {

    // 소프트 삭제(deleted_at) 정책: 조회·동반 삭제 대상은 삭제되지 않은 구간만 본다.
    List<BibleVerseVideoSegment> findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(Long sourceVideoId);

    // replaceSegments는 같은 (verse, source) 유니크 충돌을 피하려 기존 구간을 물리 삭제한 뒤 재삽입한다.
    long deleteBySourceVideo_Id(Long sourceVideoId);

    @Query("""
            select segment
              from BibleVerseVideoSegment segment
              join fetch segment.sourceVideo sourceVideo
             where segment.bibleVerseId in :verseIds
               and segment.deletedAt is null
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
