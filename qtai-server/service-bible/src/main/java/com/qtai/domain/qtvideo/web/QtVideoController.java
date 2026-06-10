package com.qtai.domain.qtvideo.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.security.AuthenticationSupport;
import com.qtai.domain.qtvideo.api.GetQtVideoUseCase;
import com.qtai.domain.qtvideo.api.dto.QtVideoClipResponse;
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
public class QtVideoController {

    private final GetQtVideoUseCase getQtVideoUseCase;

    @GetMapping("/{qtPassageId}/video")
    public ResponseEntity<ApiResponse<QtVideoClipResponse>> getVideo(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId) {
        AuthenticationSupport.requireMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success(getQtVideoUseCase.getVideo(qtPassageId)));
    }
}
