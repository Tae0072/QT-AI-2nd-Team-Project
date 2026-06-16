package com.qtai.domain.qtvideo.web;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoClipItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoClipListResponse;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSegmentItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSourceItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSourceListResponse;
import com.qtai.domain.qtvideo.api.dto.PrepareQtVideoClipResult;
import com.qtai.domain.qtvideo.internal.AdminQtVideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/qt-videos")
public class AdminQtVideoController {

    private static final ObjectMapper AUDIT_MAPPER = new ObjectMapper();
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE_SOURCE_VIDEO = "SOURCE_VIDEO";
    private static final String TARGET_TYPE_QT_VIDEO_CLIP = "QT_VIDEO_CLIP";
    private static final String TARGET_TYPE_QT_PASSAGE = "QT_PASSAGE";
    private static final String ACTION_SOURCE_VIDEO_CREATE = "QT_VIDEO_SOURCE_CREATE";
    private static final String ACTION_SOURCE_VIDEO_UPDATE = "QT_VIDEO_SOURCE_UPDATE";
    private static final String ACTION_SOURCE_VIDEO_DELETE = "QT_VIDEO_SOURCE_DELETE";
    private static final String ACTION_SEGMENTS_REPLACE = "QT_VIDEO_SEGMENTS_REPLACE";
    private static final String ACTION_CLIP_PREPARE = "QT_VIDEO_CLIP_PREPARE";
    private static final String ACTION_CLIP_STATUS_CHANGE = "QT_VIDEO_CLIP_STATUS_CHANGE";
    private static final String ACTION_CLIP_DELETE = "QT_VIDEO_CLIP_DELETE";

    private final AdminQtVideoService adminQtVideoService;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ListBibleBooksUseCase listBibleBooksUseCase;

    public AdminQtVideoController(
            AdminQtVideoService adminQtVideoService,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            WriteAuditLogUseCase auditLogUseCase,
            ListBibleBooksUseCase listBibleBooksUseCase
    ) {
        this.adminQtVideoService = adminQtVideoService;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
        this.auditLogUseCase = auditLogUseCase;
        this.listBibleBooksUseCase = listBibleBooksUseCase;
    }

    @GetMapping("/bible-books")
    public ResponseEntity<ApiResponse<List<BibleBookResponse>>> listBibleBooks(Authentication authentication) {
        requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(listBibleBooksUseCase.listBibleBooks()));
    }

    @GetMapping("/source-videos")
    public ResponseEntity<ApiResponse<AdminQtVideoSourceListResponse>> listSourceVideos(
            Authentication authentication,
            @RequestParam(required = false) Short bibleBookId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                adminQtVideoService.listSourceVideos(bibleBookId, status, page, size)
        ));
    }

    @PostMapping("/source-videos")
    public ResponseEntity<ApiResponse<AdminQtVideoSourceItem>> createSourceVideo(
            Authentication authentication,
            @Valid @RequestBody SourceVideoRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        AdminQtVideoSourceItem result = adminQtVideoService.createSourceVideo(
                request.bibleBookId(),
                request.title(),
                request.videoUrl(),
                request.durationSec()
        );
        Map<String, Object> after = orderedMap();
        after.put("bibleBookId", result.bibleBookId());
        writeAudit(adminUserId, ACTION_SOURCE_VIDEO_CREATE, TARGET_TYPE_SOURCE_VIDEO, result.id(),
                null, auditJson(after));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/source-videos/{sourceVideoId}")
    public ResponseEntity<ApiResponse<AdminQtVideoSourceItem>> updateSourceVideo(
            Authentication authentication,
            @PathVariable Long sourceVideoId,
            @Valid @RequestBody SourceVideoUpdateRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        AdminQtVideoSourceItem result = adminQtVideoService.updateSourceVideo(
                sourceVideoId,
                request.title(),
                request.videoUrl(),
                request.durationSec(),
                request.status()
        );
        Map<String, Object> after = orderedMap();
        after.put("status", result.status());
        writeAudit(adminUserId, ACTION_SOURCE_VIDEO_UPDATE, TARGET_TYPE_SOURCE_VIDEO, sourceVideoId,
                null, auditJson(after));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/source-videos/{sourceVideoId}")
    public ResponseEntity<Void> deleteSourceVideo(
            Authentication authentication,
            @PathVariable Long sourceVideoId
    ) {
        Long adminUserId = requireManager(authentication);
        AdminQtVideoService.DeletedSourceVideoSummary summary =
                adminQtVideoService.deleteSourceVideo(sourceVideoId);
        Map<String, Object> before = orderedMap();
        before.put("bibleBookId", summary.bibleBookId());
        before.put("status", summary.status());
        before.put("deletedClips", summary.deletedClips());
        before.put("deletedSegments", summary.deletedSegments());
        writeAudit(adminUserId, ACTION_SOURCE_VIDEO_DELETE, TARGET_TYPE_SOURCE_VIDEO, sourceVideoId,
                auditJson(before), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/source-videos/{sourceVideoId}/segments")
    public ResponseEntity<ApiResponse<List<AdminQtVideoSegmentItem>>> listSegments(
            Authentication authentication,
            @PathVariable Long sourceVideoId
    ) {
        requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.listSegments(sourceVideoId)));
    }

    @PutMapping("/source-videos/{sourceVideoId}/segments")
    public ResponseEntity<ApiResponse<List<AdminQtVideoSegmentItem>>> replaceSegments(
            Authentication authentication,
            @PathVariable Long sourceVideoId,
            @Valid @RequestBody SegmentReplaceRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        List<AdminQtVideoSegmentItem> result = adminQtVideoService.replaceSegments(
                sourceVideoId,
                request.segments().stream()
                        .map(segment -> new AdminQtVideoService.SegmentCommand(
                                segment.bibleVerseId(),
                                segment.chapter(),
                                segment.verse(),
                                segment.startTimeSec(),
                                segment.endTimeSec()
                        ))
                        .toList()
        );
        Map<String, Object> after = orderedMap();
        after.put("segmentCount", result.size());
        writeAudit(adminUserId, ACTION_SEGMENTS_REPLACE, TARGET_TYPE_SOURCE_VIDEO, sourceVideoId,
                null, auditJson(after));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/clips")
    public ResponseEntity<ApiResponse<AdminQtVideoClipListResponse>> listClips(
            Authentication authentication,
            @RequestParam(required = false) Long qtPassageId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.listClips(qtPassageId, status, page, size)));
    }

    @PostMapping("/qt-passages/{qtPassageId}/clips/prepare")
    public ResponseEntity<ApiResponse<PrepareQtVideoClipResult>> prepareClip(
            Authentication authentication,
            @PathVariable Long qtPassageId
    ) {
        Long adminUserId = requireManager(authentication);
        PrepareQtVideoClipResult result = adminQtVideoService.prepareClip(qtPassageId);
        Map<String, Object> after = orderedMap();
        after.put("qtPassageId", result.qtPassageId());
        after.put("prepared", result.prepared());
        after.put("clipId", result.clipId());
        writeAudit(adminUserId, ACTION_CLIP_PREPARE, TARGET_TYPE_QT_PASSAGE, qtPassageId,
                null, auditJson(after));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/clips/{clipId}")
    public ResponseEntity<Void> deleteClip(
            Authentication authentication,
            @PathVariable Long clipId
    ) {
        Long adminUserId = requireManager(authentication);
        AdminQtVideoService.DeletedClipSummary summary = adminQtVideoService.deleteClip(clipId);
        Map<String, Object> before = orderedMap();
        before.put("qtPassageId", summary.qtPassageId());
        before.put("status", summary.status());
        before.put("sourceVideoId", summary.sourceVideoId());
        writeAudit(adminUserId, ACTION_CLIP_DELETE, TARGET_TYPE_QT_VIDEO_CLIP, clipId,
                auditJson(before), null);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/clips/{clipId}/status")
    public ResponseEntity<ApiResponse<AdminQtVideoClipItem>> changeClipStatus(
            Authentication authentication,
            @PathVariable Long clipId,
            @Valid @RequestBody ClipStatusRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        AdminQtVideoClipItem result = adminQtVideoService.changeClipStatus(clipId, request.status());
        Map<String, Object> after = orderedMap();
        after.put("status", result.status());
        writeAudit(adminUserId, ACTION_CLIP_STATUS_CHANGE, TARGET_TYPE_QT_VIDEO_CLIP, clipId,
                null, auditJson(after));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void writeAudit(
            Long adminUserId,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson
    ) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminUserId,
                ACTOR_TYPE_ADMIN + ":" + adminUserId,
                actionType,
                targetType,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    // 감사 로그 JSON은 문자열 직접 조합 대신 Jackson으로 직렬화한다(특수문자 이스케이프·구조 안전).
    private static String auditJson(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        try {
            return AUDIT_MAPPER.writeValueAsString(fields);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize audit json. keys={}, error={}",
                    fields.keySet(), exception.getMessage());
            return null;
        }
    }

    private static Map<String, Object> orderedMap() {
        return new LinkedHashMap<>();
    }

    private Long requireManager(Authentication requestAuthentication) {
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
        return verifyAdminRoleUseCase.verifyAnyRole(
                memberId,
                List.of("OPERATOR", "REVIEWER", "CONTENT_CREATOR")
        ).adminUserId();
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

    public record SourceVideoRequest(
            @NotNull @Positive Short bibleBookId,
            @NotBlank String title,
            @NotBlank String videoUrl,
            @NotNull @Positive BigDecimal durationSec
    ) {
    }

    public record SourceVideoUpdateRequest(
            @NotBlank String title,
            @NotBlank String videoUrl,
            @NotNull @Positive BigDecimal durationSec,
            @NotBlank String status
    ) {
    }

    public record SegmentReplaceRequest(
            @NotEmpty List<@Valid SegmentRequest> segments
    ) {
    }

    public record SegmentRequest(
            Long bibleVerseId,
            Short chapter,
            Short verse,
            @NotNull BigDecimal startTimeSec,
            @NotNull BigDecimal endTimeSec
    ) {
    }

    public record ClipStatusRequest(
            @NotBlank String status
    ) {
    }
}
