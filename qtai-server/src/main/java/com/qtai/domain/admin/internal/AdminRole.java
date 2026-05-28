package com.qtai.domain.admin.internal;

/**
 * 관리자 세부 역할 enum.
 *
 * <p>ERD: admin_users.admin_role 컬럼에 매핑.
 * <p>CLAUDE.md §5: 관리자 API는 members.role=ADMIN과 admin_users.admin_role을
 * 모두 확인한 뒤, 이 네 가지 중 API 명세에 맞는 세부 권한을 요구한다.
 *
 * <p>역할 정책 (ERD §2.31):
 * <ul>
 *   <li>SUPER_ADMIN — 모든 관리 기능 + 관리자 계정 관리</li>
 *   <li>OPERATOR — 일반 운영 (신고 처리, 콘텐츠 숨김/삭제, 통계 조회)</li>
 *   <li>REVIEWER — AI 산출물 검증/승인</li>
 *   <li>CONTENT_CREATOR — 검증용 자료 제작, 내부 콘텐츠 제작</li>
 * </ul>
 */
public enum AdminRole {

    SUPER_ADMIN,
    OPERATOR,
    REVIEWER,
    CONTENT_CREATOR
}
