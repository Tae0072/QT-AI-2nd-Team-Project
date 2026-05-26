package com.qtai.domain.note.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteListResponse;

/**
 * NoteController 단위 테스트.
 *
 * MockMvc 슬라이스 대신 컨트롤러 직접 호출 + Mockito mock으로 검증한다.
 * 이유:
 * - 본 PR은 본문 미구현 스켈레톤이라 통합 동작은 다음 PR에서 검증
 * - dev permitAll 환경에서 @AuthenticationPrincipal이 null로 주입되는 시나리오와
 *   memberId null 가드, UseCase 호출 위임만 검증해도 컨트롤러 책임이 충족됨
 * - SpringBootTest는 application-dev.yml의 MySQL 의존성 때문에 무거움
 */
class NoteControllerTest {

    private ListNotesUseCase listNotesUseCase;
    private NoteController controller;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        listNotesUseCase = mock(ListNotesUseCase.class);
        controller = new NoteController(listNotesUseCase);
        defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("memberId가 null이면 UNAUTHORIZED(M0002) 던지고 UseCase 호출 없음")
    void list_whenMemberIdIsNull_throwsUnauthorized_andDoesNotInvokeUseCase() {
        assertThatThrownBy(() ->
                controller.list(null, null, null, null, defaultPageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(listNotesUseCase, never()).list(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("인증된 memberId가 있으면 UseCase에 그대로 위임하고 ApiResponse.success로 감싸 반환")
    void list_whenAuthenticated_delegatesToUseCase_andWrapsInApiResponse() {
        NoteListResponse stub = new NoteListResponse(List.of(), 0, 20, 0L, 0, true, true, "updatedAt,desc");
        when(listNotesUseCase.list(eq(1L), eq(NoteCategory.PRAYER), isNull(), isNull(), any()))
                .thenReturn(stub);

        ApiResponse<NoteListResponse> result =
                controller.list(1L, NoteCategory.PRAYER, null, null, defaultPageable);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isSameAs(stub);
        assertThat(result.error()).isNull();
        verify(listNotesUseCase, times(1)).list(eq(1L), eq(NoteCategory.PRAYER), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("UseCase가 NOT_IMPLEMENTED 던지면 컨트롤러는 그대로 전파 (GlobalExceptionHandler가 501로 변환)")
    void list_whenUseCaseThrowsNotImplemented_propagatesBusinessException() {
        when(listNotesUseCase.list(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.NOT_IMPLEMENTED, "스켈레톤"));

        assertThatThrownBy(() ->
                controller.list(1L, null, NoteStatus.SAVED, null, defaultPageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_IMPLEMENTED);
    }
}
