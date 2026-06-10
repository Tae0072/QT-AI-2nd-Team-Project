package com.qtai.domain.report.internal;

/**
 * 신고 대상 타입.
 *
 * <p>ERD: reports.target_type (VARCHAR(30)).
 * <p>신고는 공유된 콘텐츠 단위로만 가능하며, 대상은 (target_type, target_id) 쌍으로 식별한다.
 * <ul>
 *   <li>POST          — 나눔 게시글</li>
 *   <li>COMMENT       — 댓글</li>
 *   <li>AI_QA_REQUEST — AI Q&amp;A 요청/답변</li>
 *   <li>AI_ASSET      — AI 생성 산출물</li>
 * </ul>
 */
public enum ReportTargetType {
    POST,
    COMMENT,
    AI_QA_REQUEST,
    AI_ASSET
}
