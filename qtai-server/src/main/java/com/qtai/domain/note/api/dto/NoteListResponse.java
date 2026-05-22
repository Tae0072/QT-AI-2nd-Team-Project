package com.qtai.domain.note.api.dto;

import java.util.List;

public record NoteListResponse(
        List<NoteListItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

}
