package com.qtai.gateway;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Circuit Breaker 폴백 엔드포인트.
 *
 * <p>다운스트림(모놀리식/서비스) 장애·타임아웃으로 회로가 열리면 이 응답을 반환한다.
 * 모놀리식의 표준 에러 envelope({@code success/data/error{code,message}/timestamp/traceId})와
 * 동일 형태를 유지한다(클라이언트 분기 일관성). 게이트웨이는 WebFlux라 lib-common(servlet)
 * 미의존이므로 Map으로 직접 구성한다.
 */
@RestController
class GatewayFallbackController {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    @RequestMapping("/__fallback")
    Mono<ResponseEntity<Map<String, Object>>> fallback() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "C0006");
        error.put("message", "일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("data", null);
        body.put("error", error);
        body.put("timestamp", OffsetDateTime.now(SERVER_ZONE).toString());
        body.put("traceId", UUID.randomUUID().toString());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
