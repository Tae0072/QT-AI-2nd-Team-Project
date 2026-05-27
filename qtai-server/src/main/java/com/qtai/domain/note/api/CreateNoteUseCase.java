package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.api.dto.NoteSaveResponse;

public interface CreateNoteUseCase {

    NoteSaveResponse create(Long memberId, CreateNoteCommand command);
}
