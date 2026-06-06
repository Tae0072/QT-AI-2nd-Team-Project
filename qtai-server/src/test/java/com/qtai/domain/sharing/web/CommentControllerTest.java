package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.CommentUseCase;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentControllerTest {

    private CommentUseCase commentUseCase;
    private CommentController controller;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        commentUseCase = mock(CommentUseCase.class);
        controller = new CommentController(commentUseCase);
        pageable = PageRequest.of(0, 20);
    }

    @Test
    @DisplayName("작성은 인증된 memberId·postId·요청을 UseCase로 위임하고 201로 응답한다")
    void create_delegates() {
        CommentResponse stub = new CommentResponse(410L, 1L, 10L, "하늘QT", "본문", true, null);
        when(commentUseCase.create(eq(10L), eq(1L), any())).thenReturn(stub);

        ApiResponse<CommentResponse> response = controller.create(10L, 1L, new CommentCreateRequest("본문"));

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(stub);
        verify(commentUseCase).create(eq(10L), eq(1L), any());
    }

    @Test
    @DisplayName("작성은 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void create_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.create(null, 1L, new CommentCreateRequest("본문")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(commentUseCase, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("목록은 인증된 memberId·postId·pageable을 UseCase로 위임한다")
    void get_delegates() {
        CommentListResponse stub = new CommentListResponse(List.of(), 0, 20, 0L, 0, true, true);
        when(commentUseCase.list(eq(10L), eq(1L), any(Pageable.class))).thenReturn(stub);

        ApiResponse<CommentListResponse> response = controller.get(10L, 1L, pageable);

        assertThat(response.data()).isSameAs(stub);
        verify(commentUseCase).list(eq(10L), eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("목록도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void get_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.get(null, 1L, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(commentUseCase, never()).list(any(), any(), any());
    }

    @Test
    @DisplayName("삭제는 인증된 memberId·commentId를 UseCase로 위임한다(void/204)")
    void delete_delegates() {
        controller.delete(10L, 410L);

        verify(commentUseCase).delete(10L, 410L);
    }

    @Test
    @DisplayName("삭제도 memberId가 없으면 UNAUTHORIZED로 거부한다")
    void delete_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.delete(null, 410L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(commentUseCase, never()).delete(any(), any());
    }
}
