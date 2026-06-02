package com.qtai.domain.audit.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogFilter;
import com.qtai.domain.audit.api.dto.AuditLogResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 REST 엔드포인트. base path: /api/v1/audit
 *
 * <p>ROLE_ADMIN 필수 — 일반 사용자가 자신의 로그를 직접 조회하는 기능은 제공하지 않는다.
 *
 * <ul>
 *   <li>GET / → 감사 로그 목록 조회 (행위자/액션/기간 필터 + 페이징, 최신순)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final ListAuditUseCase listAuditUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> page = listAuditUseCase.list(
                new AuditLogFilter(actorType, actorId, actionType, from, to), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
