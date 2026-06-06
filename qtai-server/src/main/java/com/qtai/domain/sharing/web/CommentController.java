package com.qtai.domain.sharing.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.CommentUseCase;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }

    @PostMapping("/sharing-posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> create(
            @AuthenticationPrincipal Long memberId, @PathVariable("postId") Long postId,
            @Valid @RequestBody CommentCreateRequest request) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(commentUseCase.create(authenticatedMemberId, postId, request));

    }

    @GetMapping("/sharing-posts/{postId}/comments")
    public ApiResponse<CommentListResponse> get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(commentUseCase.list(authenticatedMemberId, postId, pageable));
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("commentId") Long commentId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        commentUseCase.delete(authenticatedMemberId, commentId);
    }

}
