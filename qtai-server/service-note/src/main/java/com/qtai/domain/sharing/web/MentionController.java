package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.ListMentionsUseCase;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내가 태그(멘션)된 글 목록 REST 엔드포인트.
 *
 * GET /api/v1/me/mentions — 내가 '#닉네임'으로 멘션된 나눔 글(피드와 동일 카드, 최근 글 순).
 * 좋아요/저장(BookmarkController)과 같은 인증·응답 규칙을 따른다.
 */
@RestController
@RequiredArgsConstructor
public class MentionController {

    private final ListMentionsUseCase listMentionsUseCase;

    @GetMapping("/api/v1/me/mentions")
    public ApiResponse<SharingPostListResponse> listMentions(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listMentionsUseCase.listMentions(authenticatedMemberId, pageable));
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
