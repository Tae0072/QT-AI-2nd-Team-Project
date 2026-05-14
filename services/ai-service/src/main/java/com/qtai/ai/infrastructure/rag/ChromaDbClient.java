package com.qtai.ai.infrastructure.rag;

/**
 * @deprecated ADR-0013 (2026-05-14): ChromaDB / 벡터 DB / RAG 폐기.
 * AI 응답 컨텍스트는 사전 적재된 {@code bible_explanations}(source_type = REFERENCE_SOURCE) row를 참조한다.
 * 본 파일은 삭제 권한 문제로 빈 셸만 남겨두며, @Service를 두지 않아 컴포넌트 스캔 대상에서 제외된다.
 * 추후 git rm 예정.
 *
 * <p>금지: 본 클라이언트를 다시 살려서 사용하면 AGENTS.md 금지 패턴 위반.
 */
@Deprecated
public final class ChromaDbClient {
    private ChromaDbClient() {}
}
