package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.DeleteSharingPostUseCase;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListMySharingPostsUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.PublishNoteUseCase;
import com.qtai.domain.sharing.api.SharingPostVisibilityUseCase;
import com.qtai.domain.sharing.api.ToggleLikeUseCase;
import com.qtai.domain.sharing.api.dto.LikeResponse;
import com.qtai.domain.sharing.api.dto.MySharingPostListResponse;
import com.qtai.domain.sharing.api.dto.PublishNoteRequest;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import com.qtai.domain.sharing.api.dto.VerseSnapshotDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharingPostControllerTest {

    private ListSharingPostsUseCase listSharingPostsUseCase;
    private ListMySharingPostsUseCase listMySharingPostsUseCase;
    private GetSharingPostUseCase getSharingPostUseCase;
    private PublishNoteUseCase publishNoteUseCase;
    private ToggleLikeUseCase toggleLikeUseCase;
    private DeleteSharingPostUseCase deleteSharingPostUseCase;
    private SharingPostVisibilityUseCase sharingPostVisibilityUseCase;
    private SharingPostController controller;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        listSharingPostsUseCase = mock(ListSharingPostsUseCase.class);
        listMySharingPostsUseCase = mock(ListMySharingPostsUseCase.class);
        getSharingPostUseCase = mock(GetSharingPostUseCase.class);
        publishNoteUseCase = mock(PublishNoteUseCase.class);
        toggleLikeUseCase = mock(ToggleLikeUseCase.class);
        deleteSharingPostUseCase = mock(DeleteSharingPostUseCase.class);
        sharingPostVisibilityUseCase = mock(SharingPostVisibilityUseCase.class);
        controller = new SharingPostController(
                listSharingPostsUseCase, listMySharingPostsUseCase, getSharingPostUseCase, publishNoteUseCase,
                toggleLikeUseCase, deleteSharingPostUseCase, sharingPostVisibilityUseCase);
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    @Test
    @DisplayName("목록 조회는 인증된 memberId와 필터를 UseCase로 위임한다")
    void list_delegates() {
        SharingPostListResponse expected = new SharingPostListResponse(
                List.of(), 0, 20, 0L, 0, true, true, "publishedAt,desc");
        when(listSharingPostsUseCase.list(eq(1L), eq("MEDITATION"), eq("창조"), any(Pageable.class)))
                .thenReturn(expected);

        ApiResponse<SharingPostListResponse> response = controller.list(1L, "MEDITATION", "창조", pageable);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(expected);
        verify(listSharingPostsUseCase).list(eq(1L), eq("MEDITATION"), eq("창조"), any(Pageable.class));
    }

    @Test
    @DisplayName("memberId가 없으면 UNAUTHORIZED로 거부하고 UseCase를 호출하지 않는다")
    void list_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.list(null, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(listSharingPostsUseCase, never()).list(any(), any(), any(), any());
    }

    @Test
    @DisplayName("내 나눔 목록은 인증된 memberId·status·pageable을 UseCase로 위임한다")
    void listMine_delegates() {
        MySharingPostListResponse expected = new MySharingPostListResponse(
                List.of(), 0, 20, 0L, 0, true, true, "publishedAt,desc");
        when(listMySharingPostsUseCase.listMine(eq(1L), eq("HIDDEN"), any(Pageable.class))).thenReturn(expected);

        ApiResponse<MySharingPostListResponse> response = controller.listMine(1L, "HIDDEN", pageable);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(expected);
        verify(listMySharingPostsUseCase).listMine(eq(1L), eq("HIDDEN"), any(Pageable.class));
    }

    @Test
    @DisplayName("내 나눔 목록도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void listMine_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.listMine(null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(listMySharingPostsUseCase, never()).listMine(any(), any(), any());
    }

    @Test
    @DisplayName("상세 조회는 인증된 memberId와 postId를 UseCase로 위임한다")
    void get_delegates() {
        SharingPostResponse expected = new SharingPostResponse(
                300L, 200L, 10L, "하늘QT", "오늘의 묵상", "본문 전체", "MEDITATION",
                new VerseSnapshotDetail("창세기 1:1-5", List.of()),
                true, null, "PUBLISHED", 5, 2, true, false, null, null, null);
        when(getSharingPostUseCase.getDetail(eq(1L), eq(300L))).thenReturn(expected);

        ApiResponse<SharingPostResponse> response = controller.get(1L, 300L);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(expected);
        verify(getSharingPostUseCase).getDetail(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("상세 조회도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void get_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.get(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(getSharingPostUseCase, never()).getDetail(any(), any());
    }

    @Test
    @DisplayName("공개는 인증된 memberId·noteId·요청을 UseCase로 위임하고 201로 응답한다")
    void publish_delegates() {
        SharingPostResponse stub = new SharingPostResponse(
                300L, 200L, 1L, "하늘QT", "오늘의 묵상", "본문", "MEDITATION",
                null, true, null, "PUBLISHED", 0, 0, false, true, null, null, null);
        when(publishNoteUseCase.publish(eq(1L), eq(200L), any())).thenReturn(stub);

        var response = controller.publish(1L, 200L, new PublishNoteRequest(true, true));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().data()).isSameAs(stub);
        verify(publishNoteUseCase).publish(eq(1L), eq(200L), any());
    }

    @Test
    @DisplayName("좋아요는 인증된 memberId·postId를 UseCase로 위임하고 LikeResponse를 감싸 반환한다(201)")
    void like_delegates() {
        LikeResponse expected = new LikeResponse(1L, true);
        when(toggleLikeUseCase.like(eq(1L), eq(300L))).thenReturn(expected);

        ApiResponse<LikeResponse> response = controller.like(1L, 300L);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(expected);
        verify(toggleLikeUseCase).like(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("좋아요는 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void like_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.like(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(toggleLikeUseCase, never()).like(any(), any());
    }

    @Test
    @DisplayName("좋아요 취소는 인증된 memberId·postId를 UseCase로 위임한다(void/204)")
    void unlike_delegates() {
        controller.unlike(1L, 300L);

        verify(toggleLikeUseCase).unlike(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("좋아요 취소도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void unlike_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.unlike(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(toggleLikeUseCase, never()).unlike(any(), any());
    }

    @Test
    @DisplayName("삭제는 인증된 memberId·postId를 UseCase로 위임한다(void/204)")
    void delete_delegates() {
        controller.delete(1L, 300L);

        verify(deleteSharingPostUseCase).delete(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("삭제도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void delete_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.delete(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(deleteSharingPostUseCase, never()).delete(any(), any());
    }

    @Test
    @DisplayName("숨김은 인증된 memberId·postId를 UseCase로 위임한다(void/204)")
    void hide_delegates() {
        controller.hide(1L, 300L);

        verify(sharingPostVisibilityUseCase).hide(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("숨김도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void hide_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.hide(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(sharingPostVisibilityUseCase, never()).hide(any(), any());
    }

    @Test
    @DisplayName("되돌리기는 인증된 memberId·postId를 UseCase로 위임한다(void/204)")
    void show_delegates() {
        controller.show(1L, 300L);

        verify(sharingPostVisibilityUseCase).show(eq(1L), eq(300L));
    }

    @Test
    @DisplayName("되돌리기도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void show_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.show(null, 300L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(sharingPostVisibilityUseCase, never()).show(any(), any());
    }
}
