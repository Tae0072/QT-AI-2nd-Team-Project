package com.qtai.domain.ai.client.qt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import org.springframework.stereotype.Component;

/**
 * qt 도메인 {@link GetQtPassageContentContextUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>QT 본문 컨텍스트 조회는 bible(콘텐츠) 서비스 소관이라, 미통합 구간에서는 본문 없음으로 둔다.
 * 이 흐름(해설 생성 job 처리·00:05 시딩)은 {@code ai.scheduling.enabled=true}일 때만 동작하며
 * 테스트·기동 검증에서는 비활성이다. 통합 시 RestClient 어댑터로 교체하고 이 Mock은 삭제한다.
 */
@Component("aiGetQtPassageContentContextUseCaseMock")
public class GetQtPassageContentContextUseCaseMock implements GetQtPassageContentContextUseCase {

    @Override
    public QtPassageContentContext getContentContext(Long qtPassageId) {
        return new QtPassageContentContext(qtPassageId, null, null, List.of(), false);
    }

    @Override
    public Optional<QtPassageContentContext> findContentContextByDate(LocalDate qtDate) {
        return Optional.empty();
    }
}
