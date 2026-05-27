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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.CreateNoteUseCase;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteCreateRequest;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteResponse;

/**
 * NoteController 단위 테스트.
 *
 * MockMvc 슬라이스 대신 컨트롤러 직접 호출 + Mockito mock으로 검증한다.
 * 이유:
 * - 컨트롤러 책임(memberId null 가드, UseCase 위임, 예외 전파)만 격리 검증
 * - dev permitAll 환경에서 @AuthenticationPrincipal이 null로 주입되는 시나리오 포함
 * - 진짜 DB·Spring 컨텍스트가 필요한 동작은 NoteRepositoryIntegrationTest(@DataJpaTest + H2)에서 별도 검증
 */
class NoteControllerTest {

    private ListNotesUseCase listNotesUseCase;
    private CreateNoteUseCase createNoteUseCase;
    private NoteController controller;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        listNotesUseCase = mock(ListNotesUseCase.class);
        createNoteUseCase = mock(CreateNoteUseCase.class);
        controller = new NoteController(listNotesUseCase, createNoteUseCase);
        defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("memberId가 null이면 UNAUTHORIZED(M0002) 던지고 UseCase 호출 없음")
    void list_memberId_null이면_401() {
        // when & then — null memberId로 호출하면 UNAUTHORIZED
        assertThatThrownBy(() ->
                controller.list(null, null, null, null, defaultPageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        // then — UseCase는 호출되지 않음
        verify(listNotesUseCase, never()).list(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("인증된 memberId가 있으면 UseCase에 그대로 위임하고 ApiResponse.success로 감싸 반환")
    void list_정상위임_ApiResponse_포장() {
        // given
        NoteListResponse stub = new NoteListResponse(List.of(), 0, 20, 0L, 0, true, true, "updatedAt,desc");
        when(listNotesUseCase.list(eq(1L), eq(NoteCategory.PRAYER), isNull(), isNull(), any()))
                .thenReturn(stub);

        // when
        ApiResponse<NoteListResponse> result =
                controller.list(1L, NoteCategory.PRAYER, null, null, defaultPageable);

        // then — ApiResponse 포장 확인
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isSameAs(stub);
        assertThat(result.error()).isNull();

        // then — UseCase가 정확한 인자로 1번 호출됨
        verify(listNotesUseCase, times(1)).list(eq(1L), eq(NoteCategory.PRAYER), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("UseCase가 BusinessException 던지면 컨트롤러는 catch 없이 그대로 전파 (GlobalExceptionHandler가 처리)")
    void list_BusinessException_그대로_전파() {
        // given — UseCase가 INTERNAL_ERROR 던지도록 설정
        when(listNotesUseCase.list(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "DB 조회 실패"));

        // when & then — 컨트롤러는 잡지 않고 위로 던짐
        assertThatThrownBy(() ->
                controller.list(1L, null, NoteStatus.SAVED, null, defaultPageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("POST /api/v1/notes는 인증된 memberId와 요청 DTO를 CreateNoteUseCase에 위임하고 201 Created로 감싸 반환")
    void create_정상위임_201_ApiResponse_포장() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "주일 설교",
                "본문",
                List.of(3L, 5L),
                NoteStatus.SAVED
        );
        NoteResponse stub = new NoteResponse(
                99L,
                NoteCategory.SERMON,
                NoteStatus.SAVED,
                "PRIVATE",
                "주일 설교",
                "본문",
                List.of(3L, 5L),
                null,
                null
        );
        when(createNoteUseCase.create(1L, request)).thenReturn(stub);

        // when
        ResponseEntity<ApiResponse<NoteResponse>> result = controller.create(1L, request);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().success()).isTrue();
        assertThat(result.getBody().data()).isSameAs(stub);
        verify(createNoteUseCase).create(1L, request);
    }

    @Test
    @DisplayName("POST /api/v1/notes에서 memberId가 null이면 UNAUTHORIZED(M0002) 던지고 UseCase 호출 없음")
    void create_memberId_null이면_401() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "주일 설교",
                "본문",
                List.of(3L),
                NoteStatus.SAVED
        );

        // when & then
        assertThatThrownBy(() -> controller.create(null, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(createNoteUseCase, never()).create(any(), any());
    }
}
