package com.qtai.domain.ai.client.qt;

/**
 * qt.GetQtUseCase 임시 구현체 (Mock).
 *
 * AI 도메인이 qt 컨텍스트(본문·성경 절)를 조회할 때 사용. qt 도메인 완성 전까지
 * 개발이 블로킹되지 않도록 고정 더미 데이터를 반환. 실제 QtService에 @Primary가
 * 붙으면 이 Mock은 자동 비활성화된다.
 */
// TODO: @Component
// TODO: implements GetQtUseCase
public class GetQtUseCaseMock {

    // TODO: getQt(viewerId, qtId) → 더미 QtResponse 반환 (id, content="...", verseRef="요한복음 3:16")
}
