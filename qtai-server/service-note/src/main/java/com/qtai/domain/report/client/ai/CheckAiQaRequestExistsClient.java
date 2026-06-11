package com.qtai.domain.report.client.ai;

public interface CheckAiQaRequestExistsClient {

    /**
     * 현재 요청의 Authorization 헤더 기준으로 AI Q&A 요청이 조회 가능한지 확인한다.
     *
     * @param memberId 신고자 ID. 호출자 계약 검증용이며, 원격 가시성 판단은 전달된 사용자 토큰이 수행한다.
     * @param requestId AI Q&A 요청 ID
     */
    boolean exists(Long memberId, Long requestId);
}
