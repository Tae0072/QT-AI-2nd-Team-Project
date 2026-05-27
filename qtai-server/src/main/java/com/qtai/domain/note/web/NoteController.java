package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.CreateNoteUseCase;
import com.qtai.domain.note.api.DeleteNoteUseCase;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.UpdateNoteUseCase;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteSaveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final ListNotesUseCase listNotesUseCase;
    private final GetNoteUseCase getNoteUseCase;
    private final CreateNoteUseCase createNoteUseCase;
    private final UpdateNoteUseCase updateNoteUseCase;
    private final DeleteNoteUseCase deleteNoteUseCase;

    @GetMapping
    public ApiResponse<NoteListResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) NoteCategory category,
            @RequestParam(required = false) NoteStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listNotesUseCase.list(authenticatedMemberId, category, status, q, pageable));
    }

    @GetMapping("/draft")
    public ApiResponse<NoteDraftResponse> getDraft(
            @AuthenticationPrincipal Long memberId,
            @RequestParam NoteCategory category,
            @RequestParam Long qtPassageId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(getNoteUseCase.getDraft(authenticatedMemberId, category, qtPassageId));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<NoteDetailResponse> get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long noteId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(getNoteUseCase.get(authenticatedMemberId, noteId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoteSaveResponse> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateNoteRequest request) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(createNoteUseCase.create(authenticatedMemberId, request.toCommand()));
    }

    @PatchMapping("/{noteId}")
    public ApiResponse<NoteSaveResponse> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long noteId,
            @Valid @RequestBody UpdateNoteRequest request) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(updateNoteUseCase.update(authenticatedMemberId, noteId, request.toCommand()));
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long noteId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        deleteNoteUseCase.delete(authenticatedMemberId, noteId);
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
