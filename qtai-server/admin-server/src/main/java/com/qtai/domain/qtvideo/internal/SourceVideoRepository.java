package com.qtai.domain.qtvideo.internal;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SourceVideoRepository extends JpaRepository<SourceVideo, Long> {

    Page<SourceVideo> findByBibleBookId(Short bibleBookId, Pageable pageable);

    Page<SourceVideo> findByStatus(SourceVideoStatus status, Pageable pageable);

    Page<SourceVideo> findByBibleBookIdAndStatus(Short bibleBookId, SourceVideoStatus status, Pageable pageable);

    Optional<SourceVideo> findByBibleBookIdAndActiveUniqueKey(Short bibleBookId, String activeUniqueKey);
}
