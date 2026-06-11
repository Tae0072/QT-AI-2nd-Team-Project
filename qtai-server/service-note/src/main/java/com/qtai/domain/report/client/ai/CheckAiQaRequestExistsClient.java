package com.qtai.domain.report.client.ai;

public interface CheckAiQaRequestExistsClient {

    boolean exists(Long memberId, Long requestId);
}
