package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.ProfilePhotoView;

/**
 * 본인 프로필 사진 바이트 조회 UseCase 포트(스트리밍용).
 */
public interface GetProfilePhotoUseCase {

    /** 본인 프로필 사진 바이트 조회. 사진이 없으면 RESOURCE_NOT_FOUND. */
    ProfilePhotoView getOwnProfilePhoto(Long memberId);
}
