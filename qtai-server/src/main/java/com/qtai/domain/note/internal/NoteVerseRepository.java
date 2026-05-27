package com.qtai.domain.note.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteVerseRepository extends JpaRepository<NoteVerse, Long> {

    List<NoteVerse> findByNoteIdOrderByDisplayOrderAsc(Long noteId);

    void deleteByNoteId(Long noteId);
}
