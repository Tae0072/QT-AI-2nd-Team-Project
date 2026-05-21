package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberSettingsResponse;
import com.qtai.domain.member.api.dto.MemberSettingsUpdateRequest;

/**
 * 사용자 설정 수정 UseCase 포트.
 *
 * PATCH /api/v1/me/settings (API 명세서 §4.1.7)
 * Partial Update — 요청 바디에 포함된 필드만 수정한다.
 * 인증된 본인만 호출 가능. 미인증 시 401.
 */
public interface UpdateMemberSettingsUseCase {

    // TODO: MemberSettingsResponse updateSettings(Long memberId, MemberSettingsUpdateRequest request);
    //       null 필드는 기존 값 유지.
    //       유효하지 않은 enum 값 → throw BusinessException(VALIDATION_ERROR).
    MemberSettingsResponse updateSettings(Long memberId, MemberSettingsUpdateRequest request);
}
