package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.internal.NoteCategory;
import com.qtai.domain.note.internal.NoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노트 REST 엔드포인트. base path: /api/v1/notes
 *
 * 엔드포인트 (점진적 추가):
 * GET / → 노트 목록 조회 (현재 구현 중)
 * POST / → 자유 노트 작성 (W2)
 * POST /{id}/share → 노트 공유 (W3)
 * GET /{id} → 노트 단건 조회 (W2)
 * PATCH /{id} → 노트 수정 (W2)
 * DELETE /{id} → 노트 삭제 (W2)
 */
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final ListNotesUseCase listNotesUseCase;
    // TODO: W2 진행하면서 CreateNoteUseCase, GetNoteUseCase, UpdateNoteUseCase,
    // DeleteNoteUseCase 주입

    /**
     * 노트 목록 조회. (04 API §4.3.1)
     *
     * 본인 노트 + 삭제되지 않은 것만 + 선택 필터(category/status/q) + 페이지네이션.
     * 기본 정렬: updatedAt 내림차순.
     */
    @GetMapping
    public ApiResponse<NoteListResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) NoteCategory category,
            @RequestParam(required = false) NoteStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        NoteListResponse response = listNotesUseCase.list(memberId, category, status, q, pageable);
        return ApiResponse.success(response);
    }
}
