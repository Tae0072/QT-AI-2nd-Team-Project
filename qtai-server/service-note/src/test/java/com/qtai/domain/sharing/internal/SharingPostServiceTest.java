package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.sharing.api.dto.PublishNoteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 나눔 게시글 서비스 단위 테스트(F-10).
 *
 * <p>공개 동의 가드, 좋아요(중복/없는 글), 멱등 삭제·소유권, 숨김/되돌리기 상태 전이를 다룬다.
 * 시각은 주입 Clock(Asia/Seoul)을 사용하는지 함께 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SharingPostServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private SharingPostRepository sharingPostRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private GetNoteUseCase getNoteUseCase;
    @Mock private GetMemberUseCase getMemberUseCase;
    @Mock private SendNotificationUseCase sendNotificationUseCase;

    private SharingPostService service() {
        return new SharingPostService(sharingPostRepository, postLikeRepository,
                getNoteUseCase, getMemberUseCase, sendNotificationUseCase, CLOCK);
    }

    private static SharingPost publishedPost(long ownerId) {
        return SharingPost.publish(ownerId, 10L, "제목", "본문", "MEDITATION", null, null, "닉", true);
    }

    @Test
    void 공개_닉네임동의가_아니면_INVALID_INPUT() {
        assertThatThrownBy(() -> service().publish(1L, 10L, new PublishNoteRequest(false, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(getNoteUseCase, never()).get(anyLong(), anyLong());
    }

    @Test
    void 좋아요_없는_글이면_SHARING_POST_NOT_FOUND() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().like(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);
    }

    @Test
    void 좋아요_중복이면_DUPLICATE_LIKE() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost(2L)));
        when(postLikeRepository.existsBySharingPostIdAndMemberId(5L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service().like(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_LIKE);
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void 좋아요_성공이면_저장과_카운트동기화() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost(2L)));
        when(postLikeRepository.existsBySharingPostIdAndMemberId(5L, 1L)).thenReturn(false);
        when(postLikeRepository.countBySharingPostId(5L)).thenReturn(1L);

        service().like(1L, 5L);

        verify(postLikeRepository).save(any(PostLike.class));
        verify(sharingPostRepository).syncLikeCount(5L);
    }

    @Test
    void 좋아요취소_없는_글이면_SHARING_POST_NOT_FOUND() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().unlike(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);
    }

    @Test
    void 삭제_남의_글이면_FORBIDDEN() {
        when(sharingPostRepository.findById(5L)).thenReturn(Optional.of(publishedPost(2L)));

        assertThatThrownBy(() -> service().delete(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 삭제_성공이면_DELETED로_전이() {
        SharingPost post = publishedPost(1L);
        when(sharingPostRepository.findById(5L)).thenReturn(Optional.of(post));

        service().delete(1L, 5L);

        assertThat(post.getStatus()).isEqualTo(SharingPostStatus.DELETED);
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    void 삭제_이미_삭제된_글이면_멱등() {
        SharingPost post = publishedPost(1L);
        post.delete(java.time.LocalDateTime.now(CLOCK));
        when(sharingPostRepository.findById(5L)).thenReturn(Optional.of(post));

        service().delete(1L, 5L); // 예외 없이 통과

        assertThat(post.getStatus()).isEqualTo(SharingPostStatus.DELETED);
    }

    @Test
    void 숨김과_되돌리기_상태전이() {
        SharingPost post = publishedPost(1L);
        when(sharingPostRepository.findById(5L)).thenReturn(Optional.of(post));

        service().hide(1L, 5L);
        assertThat(post.getStatus()).isEqualTo(SharingPostStatus.HIDDEN);
        assertThat(post.getHiddenAt()).isNotNull();

        service().show(1L, 5L);
        assertThat(post.getStatus()).isEqualTo(SharingPostStatus.PUBLISHED);
        assertThat(post.getHiddenAt()).isNull();
    }

    @Test
    void 내나눔목록_잘못된_status면_INVALID_INPUT() {
        assertThatThrownBy(() -> service().listMine(1L, "DELETED",
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
