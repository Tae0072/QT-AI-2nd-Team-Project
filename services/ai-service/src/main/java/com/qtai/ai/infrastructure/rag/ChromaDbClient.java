package com.qtai.ai.infrastructure.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * ChromaDB REST adapter.
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

    public Object query(String collectionName, String queryText, int nResults) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
