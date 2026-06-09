package com.qtai.domain.study.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.security.AuthenticationSupport;
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

/**
 * QT 학습 콘텐츠(해설·시뮬레이터) REST 엔드포인트. base path: /api/v1/qt
 *
 * <ul>
 *   <li>GET /{qtPassageId}/study-content              → 승인 해설 목록(F-04 계열)</li>
 *   <li>GET /{qtPassageId}/simulator                  → 시뮬레이터 상태/클립</li>
 *   <li>GET /{qtPassageId}/simulator-clips/{clipId}   → 특정 시뮬레이터 클립</li>
 * </ul>
 *
 * <p>모두 인증 필요. 인증 확인은 공통 {@link AuthenticationSupport#requireMemberId(Long)}로 통일.
 */
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
        AuthenticationSupport.requireMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtStudyContentUseCase.getStudyContent(qtPassageId)));
    }

    @GetMapping("/{qtPassageId}/simulator")
    public ResponseEntity<ApiResponse<QtSimulatorResponse>> getSimulator(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId) {
        AuthenticationSupport.requireMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtSimulatorUseCase.getSimulator(qtPassageId)));
    }

    @GetMapping("/{qtPassageId}/simulator-clips/{clipId}")
    public ResponseEntity<ApiResponse<QtSimulatorResponse>> getSimulatorClip(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId,
            @PathVariable Long clipId) {
        AuthenticationSupport.requireMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtSimulatorUseCase.getSimulatorClip(qtPassageId, clipId)));
    }
}
