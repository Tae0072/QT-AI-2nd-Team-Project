package com.qtai.domain.report.api.dto;

/**
 * 관리자 신고 목록 조회 조건.
 *
 * @param status     상태 필터(RECEIVED/REVIEWING/RESOLVED/REJECTED), null/blank이면 전체
 * @param targetType 대상 타입 필터(POST/COMMENT/AI_QA_REQUEST/AI_ASSET), null/blank이면 전체
 * @param page       0-base 페이지
 * @param size       페이지 크기
 */
public record AdminReportListQuery(
        String status,
        String targetType,
        int page,
        int size) {
}
