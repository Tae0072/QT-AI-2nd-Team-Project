package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.CreateNoteUseCase;
import com.qtai.domain.note.api.DeleteNoteUseCase;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.ListNoteCategoriesUseCase;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.UpdateNoteUseCase;
import com.qtai.domain.note.api.dto.NoteCategoryItem;
import com.qtai.domain.note.api.dto.NoteCategoryResponse;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteSaveResponse;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteControllerTest {

    private ListNotesUseCase listNotesUseCase;
    private GetNoteUseCase getNoteUseCase;
    private CreateNoteUseCase createNoteUseCase;
    private UpdateNoteUseCase updateNoteUseCase;
    private DeleteNoteUseCase deleteNoteUseCase;
    private ListNoteCategoriesUseCase listNoteCategoriesUseCase;
    private NoteController controller;
    private NoteCategoryController categoryController;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        listNotesUseCase = mock(ListNotesUseCase.class);
        getNoteUseCase = mock(GetNoteUseCase.class);
        createNoteUseCase = mock(CreateNoteUseCase.class);
        updateNoteUseCase = mock(UpdateNoteUseCase.class);
        deleteNoteUseCase = mock(DeleteNoteUseCase.class);
        listNoteCategoriesUseCase = mock(ListNoteCategoriesUseCase.class);
        controller = new NoteController(
                listNotesUseCase,
                getNoteUseCase,
                createNoteUseCase,
                updateNoteUseCase,
                deleteNoteUseCase
        );
        categoryController = new NoteCategoryController(listNoteCategoriesUseCase);
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("list rejects missing member id")
    void list_memberIdNull_rejected() {
        assertThatThrownBy(() -> controller.list(null, null, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(listNotesUseCase, never()).list(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("list delegates filters to use case")
    void list_delegates() {
        NoteListResponse stub = new NoteListResponse(List.of(), 0, 20, 0L, 0, true, true, "updatedAt,desc");
        when(listNotesUseCase.list(eq(1L), eq(NoteCategory.PRAYER), isNull(), isNull(), any()))
                .thenReturn(stub);

        ApiResponse<NoteListResponse> response = controller.list(1L, NoteCategory.PRAYER, null, null, pageable);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(stub);
    }

    @Test
    @DisplayName("draft lookup delegates authenticated member")
    void getDraft_delegates() {
        NoteDraftResponse stub = new NoteDraftResponse(false, null);
        when(getNoteUseCase.getDraft(1L, NoteCategory.MEDITATION, 100L)).thenReturn(stub);

        ApiResponse<NoteDraftResponse> response = controller.getDraft(1L, NoteCategory.MEDITATION, 100L);

        assertThat(response.data()).isSameAs(stub);
        verify(getNoteUseCase).getDraft(1L, NoteCategory.MEDITATION, 100L);
    }

    @Test
    @DisplayName("get delegates authenticated member and note id")
    void get_delegates() {
        NoteDetailResponse stub = new NoteDetailResponse(
                10L, 1L, NoteCategory.PRAYER, null, "title", "body",
                null, null, null, null, NoteStatus.SAVED, NoteVisibility.PRIVATE,
                null, null, false, null, null, null, List.of()
        );
        when(getNoteUseCase.get(1L, 10L)).thenReturn(stub);

        ApiResponse<NoteDetailResponse> response = controller.get(1L, 10L);

        assertThat(response.data()).isSameAs(stub);
        verify(getNoteUseCase).get(1L, 10L);
    }

    @Test
    @DisplayName("create maps request to command")
    void create_delegates() {
        when(createNoteUseCase.create(eq(1L), any()))
                .thenReturn(new NoteSaveResponse(10L, NoteStatus.SAVED));
        CreateNoteRequest request = new CreateNoteRequest(
                NoteCategory.PRAYER,
                null,
                "기도",
                "본문",
                null,
                null,
                null,
                null,
                List.of(1L),
                NoteStatus.SAVED,
                NoteVisibility.PRIVATE
        );

        ApiResponse<NoteSaveResponse> response = controller.create(1L, request);

        assertThat(response.data().id()).isEqualTo(10L);
        verify(createNoteUseCase).create(eq(1L), any());
    }

    @Test
    @DisplayName("update delegates note id and command")
    void update_delegates() {
        when(updateNoteUseCase.update(eq(1L), eq(10L), any()))
                .thenReturn(new NoteSaveResponse(10L, NoteStatus.DRAFT));
        UpdateNoteRequest request = new UpdateNoteRequest(
                NoteCategory.PRAYER,
                null,
                "기도",
                "본문",
                null,
                null,
                null,
                null,
                List.of(),
                NoteStatus.DRAFT,
                NoteVisibility.PRIVATE
        );

        ApiResponse<NoteSaveResponse> response = controller.update(1L, 10L, request);

        assertThat(response.data().status()).isEqualTo(NoteStatus.DRAFT);
        verify(updateNoteUseCase).update(eq(1L), eq(10L), any());
    }

    @Test
    @DisplayName("delete delegates authenticated member and note id")
    void delete_delegates() {
        controller.delete(1L, 10L);

        verify(deleteNoteUseCase).delete(1L, 10L);
    }

    @Test
    @DisplayName("category controller rejects missing member id")
    void categories_memberIdNull_rejected() {
        assertThatThrownBy(() -> categoryController.list(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(listNoteCategoriesUseCase, never()).listCategories();
    }

    @Test
    @DisplayName("category controller delegates authenticated request")
    void categories_delegates() {
        NoteCategoryResponse stub = new NoteCategoryResponse(List.of(
                new NoteCategoryItem(NoteCategory.PRAYER, "기도 노트", false, true, true)
        ));
        when(listNoteCategoriesUseCase.listCategories()).thenReturn(stub);

        ApiResponse<NoteCategoryResponse> response = categoryController.list(1L);

        assertThat(response.data()).isSameAs(stub);
        verify(listNoteCategoriesUseCase).listCategories();
    }
}
