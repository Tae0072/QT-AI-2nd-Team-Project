package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.NoteUpdateResponse;
import com.qtai.domain.note.api.dto.UpdateNoteCommand;

public interface UpdateNoteUseCase {

    NoteUpdateResponse update(Long memberId, Long noteId, UpdateNoteCommand command);
}
