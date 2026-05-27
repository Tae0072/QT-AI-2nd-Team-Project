package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.NoteSaveResponse;
import com.qtai.domain.note.api.dto.UpdateNoteCommand;

public interface UpdateNoteUseCase {

    NoteSaveResponse update(Long memberId, Long noteId, UpdateNoteCommand command);
}
