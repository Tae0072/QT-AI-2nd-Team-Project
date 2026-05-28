package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.api.dto.NoteCreateResponse;

public interface CreateNoteUseCase {

    NoteCreateResponse create(Long memberId, CreateNoteCommand command);
}
