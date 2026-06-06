package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.sharing.api.CommentUseCase;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 도메인 서비스(F-10). 작성·목록·삭제를 담당한다.
 *
 * <p>SharingPostService가 이미 커서 댓글은 별도 서비스로 분리했다.
 * commentCount는 좋아요 likeCount와 동일하게 원자 UPDATE로 맞춘다(P1-2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService implements CommentUseCase {

    private final CommentRepository commentRepository;
    private final SharingPostRepository sharingPostRepository;
    // 다른 도메인은 api 포트로만(CLAUDE.md §4). 작성자 닉네임 조회용.
    private final GetMemberUseCase getMemberUseCase;
    // 댓글 알림 발송용(P1-13).
    private final com.qtai.domain.notification.api.SendNotificationUseCase sendNotificationUseCase;

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
        // 3. 댓글 저장 후 commentCount를 원자적으로 동기화 (P1-2 lost update 방지).
        Comment saved = commentRepository.save(Comment.of(postId, memberId, request.body()));
        sharingPostRepository.syncCommentCount(postId);
        // 4. 글 작성자에게 댓글 알림(P1-13). 본인 글 자기댓글은 제외, 실패는 비차단.
        notifyAuthorOfComment(post.getMemberId(), memberId, postId, saved.getId());
        // 5. 작성자 현재 닉네임 조회(박제 아님). 방금 내가 쓴 댓글이라 ownedByMe=true.
        String nickname = getMemberUseCase.getMemberPublic(memberId).nickname();
        return toResponse(saved, nickname, true);
    }

    /** 탈퇴·정지 등으로 공개 프로필이 없는 작성자의 표시용 닉네임. */
    static final String WITHDRAWN_MEMBER_NICKNAME = "(탈퇴한 회원)";

    /** 나눔 글 작성자에게 댓글 알림 발송. 본인 댓글 제외, 발송 실패는 비즈니스 비차단. */
    private void notifyAuthorOfComment(Long authorId, Long commenterId, Long postId, Long commentId) {
        if (authorId == null || authorId.equals(commenterId)) {
            return;
        }
        try {
            sendNotificationUseCase.send(new com.qtai.domain.notification.api.dto.NotificationSendRequest(
                    authorId, "COMMENT", "댓글 알림", "내 나눔 글에 댓글이 달렸어요.",
                    null, "SHARING_POST", postId, "COMMENT:" + commentId));
        } catch (RuntimeException e) {
            log.warn("댓글 알림 발송 실패(비차단). postId={}, errorType={}",
                    postId, e.getClass().getSimpleName());
        }
    }

    @Override
    public CommentListResponse list(Long memberId, Long postId, Pageable pageable) {
        Page<Comment> page = commentRepository
                .findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable);

        // 버그 수정(2026-06-05): 댓글마다 getMemberPublic을 호출(N+1)했고, 그 단건 계약은
        // 탈퇴 회원에 MEMBER_NOT_FOUND를 던져 탈퇴자 댓글 1건이 목록 전체를 404로 깨뜨렸다.
        // → 활성 회원 일괄 조회 1회 + 누락 id는 "(탈퇴한 회원)" 폴백.
        java.util.Set<Long> authorIds = page.getContent().stream()
                .map(Comment::getMemberId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.Map<Long, String> nicknames = getMemberUseCase.getActivePublicProfiles(authorIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.qtai.domain.member.api.dto.MemberPublicResponse::id,
                        com.qtai.domain.member.api.dto.MemberPublicResponse::nickname));

        List<CommentResponse> content = page.getContent().stream()
                .map(comment -> toResponse(
                        comment,
                        nicknames.getOrDefault(comment.getMemberId(), WITHDRAWN_MEMBER_NICKNAME),
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
            // 원자적 동기화 (P1-2). 삭제 플래그 변경(dirty)을 먼저 flush해야 정확하므로
            // syncCommentCount의 flushAutomatically=true가 markDeleted를 반영한다.
            sharingPostRepository.syncCommentCount(comment.getSharingPostId());
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
