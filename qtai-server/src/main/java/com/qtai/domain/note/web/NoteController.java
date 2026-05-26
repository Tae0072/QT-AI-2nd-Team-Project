package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteListResponse;
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
 *   GET    /          → 노트 목록 조회 (현재 PR)
 *   POST   /          → 자유 노트 작성 (다음 PR)
 *   GET    /{id}      → 노트 단건 조회 (다음 PR)
 *   PATCH  /{id}      → 노트 수정 (다음 PR)
 *   DELETE /{id}      → 노트 삭제 (다음 PR)
 *   POST   /{id}/share → 노트 공유 (W3)
 */
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final ListNotesUseCase listNotesUseCase;

    /**
     * 노트 목록 조회. (04 API §4.3.1)
     *
     * 본인 노트 + 삭제되지 않은 것만 + 선택 필터(category/status/q) + 페이지네이션.
     * 기본 정렬: updatedAt 내림차순.
     *
     * 보안: memberId가 인증되지 않으면 UNAUTHORIZED. dev 환경 permitAll로 우회된 호출도 차단.
     */
    @GetMapping
    public ApiResponse<NoteListResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) NoteCategory category,
            @RequestParam(required = false) NoteStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (memberId == null) {
            // dev permitAll 환경에서 @AuthenticationPrincipal이 null로 주입되면 본인 필터가 무력화됨.
            // 명시적으로 401을 던져 다른 사용자 노트 노출을 차단.
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        NoteListResponse response = listNotesUseCase.list(memberId, category, status, q, pageable);
        return ApiResponse.success(response);
    }
}
