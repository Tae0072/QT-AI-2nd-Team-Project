package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.ListNoteCategoriesUseCase;
import com.qtai.domain.note.api.dto.NoteCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/note-categories")
@RequiredArgsConstructor
public class NoteCategoryController {

    private final ListNoteCategoriesUseCase listNoteCategoriesUseCase;

    @GetMapping
    public ApiResponse<NoteCategoryResponse> list(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(listNoteCategoriesUseCase.listCategories());
    }
}
