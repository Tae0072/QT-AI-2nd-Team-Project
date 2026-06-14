package com.qtai.domain.member.api.dto;

/**
 * 프로필 사진 바이트 응답(스트리밍용).
 *
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다.
 */
public record ProfilePhotoView(byte[] data, String contentType) {
}
