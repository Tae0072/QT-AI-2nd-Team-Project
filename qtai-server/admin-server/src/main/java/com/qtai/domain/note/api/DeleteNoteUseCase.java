package com.qtai.domain.note.api;

public interface DeleteNoteUseCase {

    void delete(Long memberId, Long noteId);
}
