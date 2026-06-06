package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QtPassageVerseRepository extends JpaRepository<QtPassageVerse, Long> {

    List<QtPassageVerse> findByQtPassageIdOrderByDisplayOrderAsc(Long qtPassageId);

    /** 본문 매핑 교체 저장 전 기존 매핑 삭제 (import/백필 경로 전용). */
    void deleteByQtPassageId(Long qtPassageId);
}
