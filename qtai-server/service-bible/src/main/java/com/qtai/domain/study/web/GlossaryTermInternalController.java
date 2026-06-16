package com.qtai.domain.study.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/study/glossary-terms")
@RequiredArgsConstructor
public class GlossaryTermInternalController {

    private final PublishApprovedGlossaryTermsUseCase publishApprovedGlossaryTermsUseCase;
    private final HidePublishedGlossaryTermsUseCase hidePublishedGlossaryTermsUseCase;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<PublishApprovedGlossaryTermsResult> publish(
            @Valid @RequestBody PublishApprovedGlossaryTermsCommand command) {
        return ApiResponse.success(publishApprovedGlossaryTermsUseCase.publishApprovedGlossaryTerms(command));
    }

    @PostMapping("/hide")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<HidePublishedGlossaryTermsResult> hide(
            @Valid @RequestBody HidePublishedGlossaryTermsCommand command) {
        return ApiResponse.success(hidePublishedGlossaryTermsUseCase.hidePublishedGlossaryTerms(command));
    }
}
