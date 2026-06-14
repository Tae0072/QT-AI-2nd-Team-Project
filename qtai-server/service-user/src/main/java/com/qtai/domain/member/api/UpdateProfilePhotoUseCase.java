package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.MemberResponse;

/**
 * 프로필 사진 업로드/삭제 UseCase 포트(본인만).
 *
 * <p>사진 바이트는 서버 DB(members.profile_image_data)에 저장하고,
 * profile_image_url 은 스트림 경로로 채운다.
 */
public interface UpdateProfilePhotoUseCase {

    /** 프로필 사진 업로드(교체). 이미지 형식·크기 검증 후 저장한다. */
    MemberResponse updateProfilePhoto(Long memberId, byte[] data, String contentType);

    /** 프로필 사진 삭제(기본 아바타로). */
    MemberResponse deleteProfilePhoto(Long memberId);
}
