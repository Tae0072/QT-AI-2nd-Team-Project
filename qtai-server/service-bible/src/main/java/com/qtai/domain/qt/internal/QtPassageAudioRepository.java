package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QtPassageAudioRepository extends JpaRepository<QtPassageAudio, Long> {

    /**
     * 캐시 조회 — 바이트/MIME만 projection으로 가져온다(목록이 아니라 단건이지만 일관 패턴).
     */
    @Query("""
            select a.audioData as audioData, a.mimeType as mimeType, a.byteSize as byteSize
              from QtPassageAudio a
             where a.qtPassageId = :qtPassageId and a.voice = :voice
            """)
    Optional<QtPassageAudioView> findAudio(@Param("qtPassageId") Long qtPassageId,
                                           @Param("voice") String voice);
}
