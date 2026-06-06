package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private CommentRepository commentRepository;
    private SharingPostRepository sharingPostRepository;
    private GetMemberUseCase getMemberUseCase;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        sharingPostRepository = mock(SharingPostRepository.class);
        getMemberUseCase = mock(GetMemberUseCase.class);
        commentService = new CommentService(commentRepository, sharingPostRepository, getMemberUseCase);
    }

    @Test
    @DisplayName("작성 정상: 저장 + commentCount 원자 동기화 + 현재 닉네임 + ownedByMe=true")
    void create_savesAndSyncsCount() {
        SharingPost post = post(1L, true, 0);
        when(sharingPostRepository.findByIdAndStatus(1L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class)))
                .thenReturn(comment(410L, 1L, 10L, "좋은 묵상이네요", false));
        when(getMemberUseCase.getMemberPublic(10L)).thenReturn(new MemberPublicResponse(10L, "하늘QT", null));

        CommentResponse response = commentService.create(10L, 1L, new CommentCreateRequest("좋은 묵상이네요"));

        assertThat(response.id()).isEqualTo(410L);
        assertThat(response.nickname()).isEqualTo("하늘QT");
        assertThat(response.body()).isEqualTo("좋은 묵상이네요");
        assertThat(response.ownedByMe()).isTrue();
        verify(commentRepository).save(any(Comment.class));
        // P1-2: dirty-checking 대신 원자 UPDATE로 카운터 동기화
        verify(sharingPostRepository).syncCommentCount(1L);
    }

    @Test
    @DisplayName("작성 거부: 댓글 OFF 글이면 409 INVALID_STATUS_TRANSITION, 저장 안 함")
    void create_commentsDisabled_throws409() {
        SharingPost post = post(1L, false, 0); // 댓글 OFF
        when(sharingPostRepository.findByIdAndStatus(1L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.create(10L, 1L, new CommentCreateRequest("hi")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("작성 거부: 대상이 PUBLISHED가 아니면 404, 저장 안 함")
    void create_postNotPublished_throws404() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, 99L, new CommentCreateRequest("hi")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("목록: 살아있는 댓글을 현재 닉네임·ownedByMe와 함께 매핑한다")
    void list_mapsComments() {
        Comment mine = comment(1L, 1L, 10L, "내 댓글", false);
        Comment others = comment(2L, 1L, 11L, "남 댓글", false);
        when(commentRepository.findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mine, others), PageRequest.of(0, 20), 2L));
        when(getMemberUseCase.getMemberPublic(10L)).thenReturn(new MemberPublicResponse(10L, "하늘QT", null));
        when(getMemberUseCase.getMemberPublic(11L)).thenReturn(new MemberPublicResponse(11L, "은혜QT", null));

        CommentListResponse response = commentService.list(10L, 1L, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).nickname()).isEqualTo("하늘QT");
        assertThat(response.content().get(0).ownedByMe()).isTrue();  // 작성자 10 == 조회자 10
        assertThat(response.content().get(1).ownedByMe()).isFalse(); // 작성자 11 != 10
    }

    @Test
    @DisplayName("삭제 정상: 본인 댓글이면 soft delete + commentCount 원자 동기화")
    void delete_ownSoftDeletes() {
        Comment comment = comment(410L, 1L, 10L, "지울 댓글", false);
        when(commentRepository.findById(410L)).thenReturn(Optional.of(comment));

        commentService.delete(10L, 410L);

        assertThat(comment.isDeleted()).isTrue();          // 소프트 삭제됨
        // P1-2: 원자 UPDATE로 카운터 동기화 (findById→dirty checking 패턴 제거)
        verify(sharingPostRepository).syncCommentCount(1L);
    }

    @Test
    @DisplayName("삭제 거부: 남의 댓글이면 403, 삭제하지 않는다")
    void delete_notOwner_throws403() {
        Comment comment = comment(410L, 1L, 99L, "남 댓글", false); // 작성자 99
        when(commentRepository.findById(410L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(10L, 410L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThat(comment.isDeleted()).isFalse();
        verify(sharingPostRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("삭제 거부: 없는 댓글이면 404")
    void delete_notFound_throws404() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(10L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 멱등: 이미 삭제된 댓글이면 카운트 갱신 없이 조용히 통과")
    void delete_idempotent_alreadyDeleted() {
        Comment comment = comment(410L, 1L, 10L, "이미 삭제", true); // isDeleted=true
        when(commentRepository.findById(410L)).thenReturn(Optional.of(comment));

        commentService.delete(10L, 410L); // 예외 없음

        // 이미 삭제된 댓글은 카운터 동기화도 하지 않는다(멱등)
        verify(sharingPostRepository, never()).syncCommentCount(anyLong());
    }

    // ─────────────────────────────────────────────────────
    // 헬퍼 — 빌더가 없어 팩토리 + reflection으로 필드를 채운다.
    // ─────────────────────────────────────────────────────

    private static Comment comment(Long id, Long sharingPostId, Long memberId, String body, boolean deleted) {
        Comment comment = Comment.of(sharingPostId, memberId, body);
        setField(comment, "id", id);
        if (deleted) {
            comment.markDeleted();
        }
        setField(comment, "createdAt", LocalDateTime.now());
        return comment;
    }

    private static SharingPost post(Long id, boolean commentsEnabled, int commentCount) {
        SharingPost post = new SharingPost();
        setField(post, "id", id);
        setField(post, "commentsEnabled", commentsEnabled);
        setField(post, "commentCount", commentCount);
        return post;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
