package com.qtai.domain.ai.client;

public class AiClientException extends RuntimeException {

    private final FailureCode failureCode;
    private final String downstreamService;

    public AiClientException(FailureCode failureCode, String downstreamService, String message) {
        super(message);
        this.failureCode = failureCode;
        this.downstreamService = downstreamService;
    }

    public AiClientException(FailureCode failureCode, String downstreamService, String message, Throwable cause) {
        super(message, cause);
        this.failureCode = failureCode;
        this.downstreamService = downstreamService;
    }

    public FailureCode failureCode() {
        return failureCode;
    }

    public String downstreamService() {
        return downstreamService;
    }

    public boolean retryable() {
        return failureCode.retryable();
    }

    public enum FailureCode {
        TIMEOUT(true),
        RATE_LIMITED(true),
        CIRCUIT_OPEN(true),
        TEMPORARY_UNAVAILABLE(true),
        DOWNSTREAM_ERROR(true),
        UNAUTHORIZED(false),
        FORBIDDEN(false),
        NOT_FOUND(false),
        VALIDATION_FAILED(false),
        RESPONSE_MAPPING_FAILED(false);

        private final boolean retryable;

        FailureCode(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }
}
