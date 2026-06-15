package com.qtai.domain.music.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 배경음악 음원 영속성 포트.
 */
public interface MusicTrackRepository extends JpaRepository<MusicTrack, Long> {

    /** 활성 음원 목록(메타데이터만, sort_order → id 순). audio_data 는 로딩하지 않는다. */
    List<MusicTrackSummary> findByEnabledTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();

    /** 활성 음원 1건의 바이트 조회(스트리밍용). */
    @Query("select t.audioData as audioData, t.mimeType as mimeType, t.byteSize as byteSize "
            + "from MusicTrack t "
            + "where t.id = :id and t.enabled = true and t.deletedAt is null")
    Optional<MusicTrackAudioView> findEnabledAudioById(@Param("id") Long id);

    @Query("""
            select t.id as id,
                   t.title as title,
                   t.category as category,
                   t.mimeType as mimeType,
                   t.byteSize as byteSize,
                   t.durationSec as durationSec,
                   t.sortOrder as sortOrder,
                   t.enabled as enabled,
                   t.licenseNote as licenseNote,
                   t.createdAt as createdAt,
                   t.updatedAt as updatedAt
            from MusicTrack t
            where t.deletedAt is null
              and (:enabled is null or t.enabled = :enabled)
              and (:category is null or t.category = :category)
            """)
    Page<AdminMusicTrackSummary> findAdminSummaries(@Param("enabled") Boolean enabled,
                                                    @Param("category") MusicCategory category,
                                                    Pageable pageable);
}
