package com.qtai.bible.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * RFC 7807 application/problem+json 응답 표준 (DECISIONS.md §7).
 *
 * <p>구버전 ErrorResponse{code, message, traceId} 패턴 사용 금지.
 * 각 도메인에서 발생하는 비즈니스 예외는 본 클래스를 통해 ProblemDetail로 변환된다.
 */
@RestControllerAdvice
public class ProblemDetailAdvice {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest req) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "예상치 못한 오류", req);
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String code, String detail, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.qtai.app/errors/" + code.toLowerCase().replace('_', '-'));
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("code", code);
        body.put("detail", detail);
        body.put("traceId", req.getHeader("X-B3-TraceId"));
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType("application/problem+json")).body(body);
    }
}
