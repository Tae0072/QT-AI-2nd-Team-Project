package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 댓글 서비스 단위 테스트(F-10).
 *
 * <p>댓글 OFF 글 차단, 작성 시 카운트 동기화, 소유권 삭제, 그리고 탈퇴 회원 닉네임 폴백
 * (목록 1건이 전체를 404로 깨뜨리지 않는 회귀 방지)을 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private SharingPostRepository sharingPostRepository;
    @Mock private GetMemberUseCase getMemberUseCase;
    @Mock private SendNotificationUseCase sendNotificationUseCase;

    private CommentService service() {
        return new CommentService(commentRepository, sharingPostRepository,
                getMemberUseCase, sendNotificationUseCase);
    }

    private static SharingPost post(boolean commentsEnabled) {
        return SharingPost.publish(2L, 10L, "제목", "본문", "MEDITATION", null, null, "닉", commentsEnabled);
    }

    @Test
    void 댓글_없는_글이면_SHARING_POST_NOT_FOUND() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(1L, 99L, new CommentCreateRequest("내용")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);
    }

    @Test
    void 댓글_OFF_글이면_INVALID_STATUS_TRANSITION() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post(false)));

        assertThatThrownBy(() -> service().create(1L, 5L, new CommentCreateRequest("내용")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 댓글_작성_성공이면_저장과_카운트동기화() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post(true)));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(getMemberUseCase.getMemberPublic(1L)).thenReturn(new MemberPublicResponse(1L, "글쓴이", null));

        service().create(1L, 5L, new CommentCreateRequest("내용"));

        verify(commentRepository).save(any(Comment.class));
        verify(sharingPostRepository).syncCommentCount(5L);
    }

    @Test
    void 댓글_삭제_없는_댓글이면_COMMENT_NOT_FOUND() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 댓글_삭제_남의_댓글이면_FORBIDDEN() {
        when(commentRepository.findById(7L)).thenReturn(Optional.of(Comment.of(5L, 2L, "내용")));

        assertThatThrownBy(() -> service().delete(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 목록_탈퇴회원_댓글은_닉네임_폴백() {
        Comment byWithdrawn = Comment.of(5L, 99L, "탈퇴자 댓글");
        when(commentRepository.findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(byWithdrawn)));
        // 탈퇴 회원은 활성 공개프로필 일괄 조회에서 빠진다 → 폴백 대상.
        when(getMemberUseCase.getActivePublicProfiles(anyCollection())).thenReturn(List.of());

        CommentListResponse response = service().list(1L, 5L, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo(CommentService.WITHDRAWN_MEMBER_NICKNAME);
    }
}
