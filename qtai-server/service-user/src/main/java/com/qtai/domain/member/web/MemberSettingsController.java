package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.GetSettingsUseCase;
import com.qtai.domain.member.api.UpdateSettingsUseCase;
import com.qtai.domain.member.api.dto.SettingsResponse;
import com.qtai.domain.member.api.dto.SettingsUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 설정 API.
 *
 * <p>알림 수신 ON/OFF, 폰트 크기(SMALL/MEDIUM/LARGE) 설정.
 * <p>첫 조회 시 기본값 자동 생성.
 */
@RestController
@RequiredArgsConstructor
public class MemberSettingsController {

    private final GetSettingsUseCase getSettingsUseCase;
    private final UpdateSettingsUseCase updateSettingsUseCase;

    /** GET /api/v1/me/settings — 설정 조회 (없으면 기본값 자동 생성). */
    @GetMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(
            @AuthenticationPrincipal Long memberId) {
        SettingsResponse response = getSettingsUseCase.getSettings(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** PATCH /api/v1/me/settings — 설정 부분 수정. */
    @PatchMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateSettings(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SettingsUpdateRequest request) {
        SettingsResponse response = updateSettingsUseCase.updateSettings(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
