package com.qtai.domain.study.web;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.study.api.HidePublishedSimulatorClipUseCase;
import com.qtai.domain.study.api.ListAdminSimulatorClipsUseCase;
import com.qtai.domain.study.api.dto.AdminSimulatorClipListResponse;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;
import com.qtai.domain.study.api.dto.ListAdminSimulatorClipsQuery;

/**
 * 관리자 시뮬레이터 클립 관리 컨트롤러 (AD 시뮬레이터, F-06/F-12) — A1 조회+숨김.
 *
 * <p>{@code GET  /api/v1/admin/simulator-clips} — 상태/본문별 목록(메타만, sceneScript 미노출).
 * <br>{@code POST /api/v1/admin/simulator-clips/{aiAssetId}/hide} — 노출 중인 클립 숨김 + 감사 로그.
 *
 * <p>권한(CLAUDE.md §5): ROLE_ADMIN(1차) + admin_users.admin_role REVIEWER(2차, SUPER_ADMIN 우월권 포함).
 * 게시(Publish)는 AD-03 승인본 sceneScript 연동이라 후속 PR.
 */
@RestController
@RequestMapping("/api/v1/admin/simulator-clips")
public class AdminSimulatorClipController {

    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String ACTION_SIMULATOR_CLIP_HIDE = "SIMULATOR_CLIP_HIDE";
    private static final String TARGET_TYPE_AI_GENERATED_ASSET = "AI_GENERATED_ASSET";

    private final ListAdminSimulatorClipsUseCase listAdminSimulatorClipsUseCase;
    private final HidePublishedSimulatorClipUseCase hidePublishedSimulatorClipUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final WriteAuditLogUseCase auditLogUseCase;

    public AdminSimulatorClipController(
            ListAdminSimulatorClipsUseCase listAdminSimulatorClipsUseCase,
            HidePublishedSimulatorClipUseCase hidePublishedSimulatorClipUseCase,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            WriteAuditLogUseCase auditLogUseCase
    ) {
        this.listAdminSimulatorClipsUseCase = listAdminSimulatorClipsUseCase;
        this.hidePublishedSimulatorClipUseCase = hidePublishedSimulatorClipUseCase;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
        this.auditLogUseCase = auditLogUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminSimulatorClipListResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long qtPassageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireReviewer(authentication);
        AdminSimulatorClipListResponse response = listAdminSimulatorClipsUseCase.listAdminSimulatorClips(
                new ListAdminSimulatorClipsQuery(status, qtPassageId, page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{aiAssetId}/hide")
    public ResponseEntity<ApiResponse<HidePublishedSimulatorClipResult>> hide(
            @PathVariable("aiAssetId") Long aiAssetId,
            Authentication authentication
    ) {
        Long adminUserId = requireReviewer(authentication);
        HidePublishedSimulatorClipResult result = hidePublishedSimulatorClipUseCase
                .hidePublishedSimulatorClip(new HidePublishedSimulatorClipCommand(aiAssetId));
        writeHideAudit(adminUserId, aiAssetId, result);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void writeHideAudit(Long adminUserId, Long aiAssetId, HidePublishedSimulatorClipResult result) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminUserId,
                ACTOR_TYPE_ADMIN + ":" + adminUserId,
                ACTION_SIMULATOR_CLIP_HIDE,
                TARGET_TYPE_AI_GENERATED_ASSET,
                aiAssetId,
                null,
                "{\"hiddenCount\":" + result.hiddenCount() + "}"
        ));
    }

    private Long requireReviewer(Authentication requestAuthentication) {
        Authentication authentication = requestAuthentication != null
                ? requestAuthentication
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        if (!authorities.contains("ROLE_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long memberId = resolvePrincipalId(authentication);
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, List.of("REVIEWER")).adminUserId();
    }

    private static Long resolvePrincipalId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(principal));
        } catch (NumberFormatException e) {
            try {
                return Long.valueOf(authentication.getName());
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
    }
}
