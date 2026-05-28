package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
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
    private SharingPostController controller;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        listSharingPostsUseCase = mock(ListSharingPostsUseCase.class);
        controller = new SharingPostController(listSharingPostsUseCase);
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
}
