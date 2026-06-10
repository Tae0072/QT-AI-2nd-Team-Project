package com.qtai.domain.audit.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 로그 기록 — 서비스 간 <b>시스템 배치(SYSTEM_BATCH) 전용</b> 수신 엔드포인트.
 *
 * <p>다른 서비스(service-ai 등)의 AI 작업이 검수·생성 감사 기록을 admin-server(audit 소유)에 동기 기록할 때 호출한다.
 * 경로가 {@code /api/v1/system/**}이라 {@link com.qtai.security.SecurityConfig}의
 * {@code .requestMatchers("/api/v1/system/**").hasRole("SYSTEM_BATCH")} 규칙으로 시스템 배치 주체만 접근한다
 * (일반 사용자·ADMIN은 403, 미인증은 401). 시스템 토큰(HS256)은 {@link com.qtai.security.JwtAuthenticationFilter}의
 * 시스템 토큰 폴백으로 검증된다.
 *
 * <p>관리자 웹이 보는 감사 로그 <b>조회</b>는 {@link AdminAuditLogController}(/api/v1/admin/audit-logs)가 담당하고,
 * 이 컨트롤러는 시스템 주체의 <b>기록</b>만 받는다.
 */
@RestController
@RequestMapping("/api/v1/system/audit-logs")
@RequiredArgsConstructor
public class SystemAuditLogController {

    private final WriteAuditLogUseCase writeAuditLogUseCase;

    /** 감사 로그 1건을 기록한다. */
    @PostMapping
    public ApiResponse<Void> write(@RequestBody AuditLogWriteRequest request) {
        writeAuditLogUseCase.write(request);
        return ApiResponse.success(null);
    }
}
