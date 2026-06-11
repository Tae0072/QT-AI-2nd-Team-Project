package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;

public interface GetNoteUseCase {

    NoteDetailResponse get(Long memberId, Long noteId);

    NoteDraftResponse getDraft(Long memberId, NoteCategory category, Long qtPassageId);
}
