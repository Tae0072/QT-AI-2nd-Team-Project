package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.sharing.api.CommentUseCase;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 도메인 서비스(F-10). 작성·목록·삭제를 담당한다.
 *
 * <p>SharingPostService가 이미 커서 댓글은 별도 서비스로 분리했다.
 * commentCount는 좋아요 likeCount와 동일하게 COUNT 재계산으로 맞춘다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService implements CommentUseCase {

    private final CommentRepository commentRepository;
    private final SharingPostRepository sharingPostRepository;
    // 다른 도메인은 api 포트로만(CLAUDE.md §4). 작성자 닉네임 조회용.
    private final GetMemberUseCase getMemberUseCase;

    @Override
    @Transactional
    public CommentResponse create(Long memberId, Long postId, CommentCreateRequest request) {
        // 1. PUBLISHED 글에만 댓글 가능 (숨김·삭제·없는 글은 404).
        SharingPost post = sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));
        // 2. 작성자가 댓글 OFF 한 글이면 작성 차단(409).
        if (!post.isCommentsEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        // 3. 댓글 저장 후 실제 행 수로 commentCount 재계산.
        Comment saved = commentRepository.save(Comment.of(postId, memberId, request.body()));
        post.syncCommentCount(commentRepository.countBySharingPostIdAndIsDeletedFalse(postId));
        // 4. 작성자 현재 닉네임 조회(박제 아님). 방금 내가 쓴 댓글이라 ownedByMe=true.
        String nickname = getMemberUseCase.getMemberPublic(memberId).nickname();
        return toResponse(saved, nickname, true);
    }

    @Override
    public CommentListResponse list(Long memberId, Long postId, Pageable pageable) {
        Page<Comment> page = commentRepository
                .findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable);
        List<CommentResponse> content = page.getContent().stream()
                .map(comment -> toResponse(
                        comment,
                        getMemberUseCase.getMemberPublic(comment.getMemberId()).nickname(),
                        comment.getMemberId().equals(memberId)))
                .toList();
        return new CommentListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        // 본인 댓글만 삭제 가능.
        if (!comment.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 이미 삭제된 댓글이면 아무것도 안 함(멱등). 새로 삭제할 때만 카운트 갱신.
        if (!comment.isDeleted()) {
            comment.markDeleted();
            long count = commentRepository.countBySharingPostIdAndIsDeletedFalse(comment.getSharingPostId());
            sharingPostRepository.findById(comment.getSharingPostId())
                    .ifPresent(post -> post.syncCommentCount(count));
        }
    }

    /** Comment → 응답 매핑. 작성·목록이 공유한다. */
    private CommentResponse toResponse(Comment comment, String nickname, boolean ownedByMe) {
        return new CommentResponse(
                comment.getId(),
                comment.getSharingPostId(),
                comment.getMemberId(),
                nickname,
                comment.getBody(),
                ownedByMe,
                comment.getCreatedAt());
    }
}
