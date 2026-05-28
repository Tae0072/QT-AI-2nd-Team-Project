package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나눔 피드 REST 엔드포인트. base path: /api/v1/sharing-posts
 *
 * 토큰 기반 공유({@code /api/v1/shares}, SharingController)와는 별개의 커뮤니티 피드다.
 */
@RestController
@RequestMapping("/api/v1/sharing-posts")
@RequiredArgsConstructor
public class SharingPostController {

    private final ListSharingPostsUseCase listSharingPostsUseCase;
    private final GetSharingPostUseCase getSharingPostUseCase;

    @GetMapping
    public ApiResponse<SharingPostListResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listSharingPostsUseCase.list(authenticatedMemberId, category, q, pageable));
    }

    @GetMapping("/{postId}")
    public ApiResponse<SharingPostResponse> get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(getSharingPostUseCase.getDetail(authenticatedMemberId, postId));
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
