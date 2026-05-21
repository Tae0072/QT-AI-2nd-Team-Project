package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberSettingsResponse;

/**
 * 사용자 설정 조회 UseCase 포트.
 *
 * GET /api/v1/me/settings (API 명세서 §4.1.6)
 * 인증된 본인만 호출 가능. 미인증 시 401.
 */
public interface GetMemberSettingsUseCase {

    // TODO: MemberSettingsResponse getSettings(Long memberId);
    //       members 테이블(또는 member_settings) 에서 설정 값 조회.
    //       회원이 없으면 throw BusinessException(MEMBER_NOT_FOUND).
    MemberSettingsResponse getSettings(Long memberId);
}
