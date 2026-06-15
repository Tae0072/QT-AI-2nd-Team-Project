package com.qtai.domain.member.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 회원 상세 응답 DTO (F-04/F-10).
 *
 * <p>목록 정보 + 운영 판단용 집계: 작성 공유글 수, 신고한 횟수(신고자), 받은 신고 수(소유 콘텐츠 대상).
 * 개인정보(email·kakaoId) 원문은 노출하지 않는다. 닉네임 변경은 마지막 변경 시각만 보관한다
 * (전체 변경 이력 테이블은 없음 — nicknameChangedAt이 최근 변경 시각).
 */
public record AdminMemberDetailResponse(
        Long id,
        String nickname,
        String status,
        String role,
        LocalDateTime nicknameChangedAt,
        LocalDateTime withdrawnAt,
        LocalDateTime createdAt,
        long sharingPostCount,
        long reportsFiledCount,
        long reportsReceivedCount
) {
}
