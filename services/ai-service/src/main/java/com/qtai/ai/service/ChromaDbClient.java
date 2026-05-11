package com.qtai.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * ChromaDB REST 클라이언트.
 *
 * ChromaDB는 REST API 서버이며, Java에서는 Spring RestClient로 직접 호출합니다.
 * 임베딩은 ChromaDB 서버가 내장 함수로 처리하므로 자바 측에서 별도 임베딩 모델이 필요 없습니다.
 *
 * 주요 엔드포인트:
 *   POST   /api/v1/collections/{name}/query    — 유사 검색 (틅 N개)
 *   POST   /api/v1/collections/{name}/add      — 문서 추가
 *   GET    /api/v1/collections                 — 컴렉션 목록
 */
@Service
public class ChromaDbClient {

    private final RestClient restClient;

    public ChromaDbClient(
        @Value("${qtai.chromadb.host:localhost}") String host,
        @Value("${qtai.chromadb.port:8000}") int port
    ) {
        this.restClient = RestClient.builder()
            .baseUrl("http://" + host + ":" + port)
            .build();
    }

    /**
     * 컴렉션에서 query 텍스트와 유사한 상위 N개 문서 검색.
     *
     * @param collectionName 예: "bible_passages"
     * @param queryText      검색 텍스트
     * @param nResults       반환 개수
     */
    public Object query(String collectionName, String queryText, int nResults) {
        // TODO: POST /api/v1/collections/{name}/query 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
