package com.qtai.domain.note.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NoteVerseRepository extends JpaRepository<NoteVerse, Long> {

    List<NoteVerse> findAllByNoteIdOrderByDisplayOrderAsc(Long noteId);

    List<NoteVerse> findAllByNoteIdInOrderByNoteIdAscDisplayOrderAsc(Collection<Long> noteIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM NoteVerse nv WHERE nv.noteId = :noteId")
    void deleteByNoteId(@Param("noteId") Long noteId);
}
