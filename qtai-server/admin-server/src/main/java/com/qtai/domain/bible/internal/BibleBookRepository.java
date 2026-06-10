package com.qtai.domain.bible.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BibleBookRepository extends JpaRepository<BibleBook, Short> {

    List<BibleBook> findAllByOrderByDisplayOrderAsc();

    Optional<BibleBook> findByCode(String code);
}
