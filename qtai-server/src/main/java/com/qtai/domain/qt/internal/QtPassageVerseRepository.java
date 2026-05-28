package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QtPassageVerseRepository extends JpaRepository<QtPassageVerse, Long> {

    List<QtPassageVerse> findByQtPassageIdOrderByDisplayOrderAsc(Long qtPassageId);
}
