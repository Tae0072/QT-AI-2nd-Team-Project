package com.qtai.gateway.auth;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Gateway Auth 모듈의 인증 엔드포인트.
 *
 * <p>스캐폴딩: 본 컨트롤러는 시그니처와 응답 형태만 모킹한다.
 * 실제 구현은 강태오(Lead)가 다음을 채워 넣어야 한다:
 * <ul>
 *   <li>Google OAuth 콜백에서 ID Token 검증 후 USERS upsert</li>
 *   <li>RS256 Access Token (30분) + Refresh Token (14일) 발급</li>
 *   <li>Refresh Rotation + Redis 블랙리스트</li>
 * </ul>
 *
 * <p>JWK 공개키는 {@link JwksController} 가 노출한다.
 *
 * <p>참조: 05_보안_명세서.md §3, DECISIONS.md §2
 *
 * @see com.qtai.gateway.GatewayApplication
 */
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    // TODO(강태오): Google OAuth 토큰 검증 + 사용자 upsert + JWT 발급
    @PostMapping("/login/google")
    public ResponseEntity<Map<String, Object>> loginWithGoogle(@RequestBody Map<String, String> body) {
        String idToken = body.getOrDefault("idToken", "");
        // TODO 실제 구현: GoogleIdTokenVerifier → 이메일/sub 확인 → users upsert → tokenPair 발급
        return ResponseEntity.ok(Map.of(
                "accessToken", "STUB.eyJhY2Nlc3MiOiJ0b2tlbiJ9.STUB",
                "refreshToken", "STUB-REFRESH",
                "expiresIn", 1800,
                "tokenType", "Bearer",
                "issuedAt", Instant.now().toString()
        ));
    }

    // TODO(강태오): Refresh Rotation. 새 refresh 발급 후 이전 jti 블랙리스트.
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "accessToken", "STUB.access.new",
                "refreshToken", "STUB.refresh.new",
                "expiresIn", 1800,
                "tokenType", "Bearer"
        ));
    }

    // TODO(강태오): Refresh 무효화 → Redis blacklist (auth:refresh:revoked:{jti})
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.noContent().build();
    }
}
