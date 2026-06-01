package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
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
    private GetSharingPostUseCase getSharingPostUseCase;
    private com.qtai.domain.sharing.api.PublishNoteUseCase publishNoteUseCase;
    private SharingPostController controller;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        listSharingPostsUseCase = mock(ListSharingPostsUseCase.class);
        getSharingPostUseCase = mock(GetSharingPostUseCase.class);
        publishNoteUseCase = mock(com.qtai.domain.sharing.api.PublishNoteUseCase.class);
        controller = new SharingPostController(listSharingPostsUseCase, getSharingPostUseCase, publishNoteUseCase);
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

    // ── publish 테스트 ──

    @Test
    @DisplayName("publish — 정상 요청 시 201 반환")
    void publish_정상() {
        Long memberId = 1L;
        Long noteId = 10L;
        com.qtai.domain.sharing.api.dto.PublishNoteRequest request =
                new com.qtai.domain.sharing.api.dto.PublishNoteRequest(true, true);

        SharingPostResponse expected = new SharingPostResponse(
                1L, noteId, memberId, "닉네임", "제목", "본문", "MEDITATION",
                new com.qtai.domain.sharing.api.dto.VerseSnapshotDetail(null, List.of()),
                true, null, "PUBLISHED", 0, 0, false, true, null, null, null);

        when(publishNoteUseCase.publish(memberId, noteId, request)).thenReturn(expected);

        org.springframework.http.ResponseEntity<com.qtai.common.dto.ApiResponse<SharingPostResponse>> response =
                controller.publish(memberId, noteId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().data().titleSnapshot()).isEqualTo("제목");
    }

    @Test
    @DisplayName("publish — memberId null이면 UNAUTHORIZED")
    void publish_memberIdNull_rejected() {
        com.qtai.domain.sharing.api.dto.PublishNoteRequest request =
                new com.qtai.domain.sharing.api.dto.PublishNoteRequest(true, true);

        assertThatThrownBy(() -> controller.publish(null, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
