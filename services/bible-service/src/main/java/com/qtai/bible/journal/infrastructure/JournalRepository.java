package com.qtai.bible.journal.infrastructure;

import com.qtai.bible.journal.domain.Journal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    Optional<Journal> findByUserIdAndQtDate(Long userId, LocalDate qtDate);

    Page<Journal> findAllByUserIdOrderByQtDateDesc(Long userId, Pageable pageable);
}
