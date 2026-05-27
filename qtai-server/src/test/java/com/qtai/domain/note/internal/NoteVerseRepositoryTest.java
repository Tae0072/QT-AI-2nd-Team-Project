package com.qtai.domain.note.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NoteVerseRepositoryTest {

    @Autowired
    NoteVerseRepository noteVerseRepository;

    @Test
    @DisplayName("noteId 기준 displayOrder 오름차순으로 조회한다")
    void findAllByNoteIdOrderByDisplayOrderAsc_returnsOrderedVerses() {
        noteVerseRepository.saveAll(List.of(
                NoteVerse.create(1L, 30L, (short) 2),
                NoteVerse.create(1L, 20L, (short) 1),
                NoteVerse.create(2L, 99L, (short) 1)
        ));

        List<NoteVerse> result = noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(1L);

        assertThat(result)
                .extracting(NoteVerse::getBibleVerseId)
                .containsExactly(20L, 30L);
    }

    @Test
    @DisplayName("noteId 기준 verse 연결을 일괄 삭제한다")
    void deleteByNoteId_removesOnlyTargetNoteVerses() {
        noteVerseRepository.saveAll(List.of(
                NoteVerse.create(1L, 20L, (short) 1),
                NoteVerse.create(1L, 30L, (short) 2),
                NoteVerse.create(2L, 20L, (short) 1)
        ));

        noteVerseRepository.deleteByNoteId(1L);

        assertThat(noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(1L)).isEmpty();
        assertThat(noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(2L))
                .extracting(NoteVerse::getBibleVerseId)
                .containsExactly(20L);
    }
}
