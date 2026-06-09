package com.qtai.domain.ai.client.audit;

import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import org.springframework.stereotype.Component;

/**
 * audit 도메인 {@link WriteAuditLogUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>audit은 횡단 관심사라 다른 서비스로 분리된 상태에서는 동기 기록 호출을 보낼 대상이 없다.
 * 통합 단계에서 RestClient 어댑터(또는 비동기 이벤트 발행)로 교체하고 이 Mock은 삭제한다
 * (CLAUDE.md §4). 감사 기록은 fire-and-forget 성격이라 미통합 구간에서는 무해한 no-op으로 둔다.
 */
@Component("aiWriteAuditLogUseCaseMock")
public class WriteAuditLogUseCaseMock implements WriteAuditLogUseCase {

    @Override
    public void write(AuditLogWriteRequest request) {
        // 미통합 구간 no-op. 토큰·민감정보를 로그에 남기지 않기 위해 본문 로깅도 하지 않는다.
    }
}
