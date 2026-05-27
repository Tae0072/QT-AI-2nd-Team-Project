package com.qtai.domain.note.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NoteVerseRepository extends JpaRepository<NoteVerse, Long> {

    List<NoteVerse> findAllByNoteIdOrderByDisplayOrderAsc(Long noteId);

    List<NoteVerse> findAllByNoteIdInOrderByNoteIdAscDisplayOrderAsc(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);
}
