package com.qtai.domain.admin.api.dto;

/**
 * 관리자 정보 DTO.
 *
 * <p>다른 도메인에서 admin 도메인의 api/UseCase를 통해 받는 관리자 정보.
 * admin 도메인의 Entity를 외부에 노출하지 않기 위해 record DTO로 전달한다.
 *
 * @param adminUserId 관리자 계정 ID (admin_users.id)
 * @param memberId    연결 회원 ID (members.id)
 * @param adminRole   관리자 세부 역할 (SUPER_ADMIN, OPERATOR, REVIEWER, CONTENT_CREATOR)
 */
public record AdminUserInfo(
        Long adminUserId,
        Long memberId,
        String adminRole
) {
}
