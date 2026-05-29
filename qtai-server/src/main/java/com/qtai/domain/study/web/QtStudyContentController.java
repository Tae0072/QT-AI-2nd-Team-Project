package com.qtai.domain.study.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.GetQtSimulatorUseCase;
import com.qtai.domain.study.api.GetQtStudyContentUseCase;
import com.qtai.domain.study.api.dto.QtSimulatorResponse;
import com.qtai.domain.study.api.dto.QtStudyContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qt")
@RequiredArgsConstructor
public class QtStudyContentController {

    private final GetQtStudyContentUseCase getQtStudyContentUseCase;
    private final GetQtSimulatorUseCase getQtSimulatorUseCase;

    @GetMapping("/{qtPassageId}/study-content")
    public ResponseEntity<ApiResponse<QtStudyContentResponse>> getStudyContent(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId) {
        requireAuthenticated(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtStudyContentUseCase.getStudyContent(qtPassageId)));
    }

    @GetMapping("/{qtPassageId}/simulator")
    public ResponseEntity<ApiResponse<QtSimulatorResponse>> getSimulator(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId) {
        requireAuthenticated(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtSimulatorUseCase.getSimulator(qtPassageId)));
    }

    @GetMapping("/{qtPassageId}/simulator-clips/{clipId}")
    public ResponseEntity<ApiResponse<QtSimulatorResponse>> getSimulatorClip(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId,
            @PathVariable Long clipId) {
        requireAuthenticated(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtSimulatorUseCase.getSimulatorClip(qtPassageId, clipId)));
    }

    private static void requireAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
