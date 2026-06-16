package com.qtai.domain.qtvideo.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
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

@RestController
@RequestMapping("/api/v1/admin/qt-videos")
public class AdminQtVideoController {

    private final AdminQtVideoService adminQtVideoService;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final ListBibleBooksUseCase listBibleBooksUseCase;

    public AdminQtVideoController(
            AdminQtVideoService adminQtVideoService,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            ListBibleBooksUseCase listBibleBooksUseCase
    ) {
        this.adminQtVideoService = adminQtVideoService;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
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
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.createSourceVideo(
                adminUserId,
                request.bibleBookId(),
                request.title(),
                request.videoUrl(),
                request.durationSec()
        )));
    }

    @PatchMapping("/source-videos/{sourceVideoId}")
    public ResponseEntity<ApiResponse<AdminQtVideoSourceItem>> updateSourceVideo(
            Authentication authentication,
            @PathVariable Long sourceVideoId,
            @Valid @RequestBody SourceVideoUpdateRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.updateSourceVideo(
                adminUserId,
                sourceVideoId,
                request.title(),
                request.videoUrl(),
                request.durationSec(),
                request.status()
        )));
    }

    @DeleteMapping("/source-videos/{sourceVideoId}")
    public ResponseEntity<Void> deleteSourceVideo(
            Authentication authentication,
            @PathVariable Long sourceVideoId
    ) {
        Long adminUserId = requireManager(authentication);
        adminQtVideoService.deleteSourceVideo(adminUserId, sourceVideoId);
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
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.replaceSegments(
                adminUserId,
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
        )));
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
        return ResponseEntity.ok(ApiResponse.success(adminQtVideoService.prepareClip(adminUserId, qtPassageId)));
    }

    @DeleteMapping("/clips/{clipId}")
    public ResponseEntity<Void> deleteClip(
            Authentication authentication,
            @PathVariable Long clipId
    ) {
        Long adminUserId = requireManager(authentication);
        adminQtVideoService.deleteClip(adminUserId, clipId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/clips/{clipId}/status")
    public ResponseEntity<ApiResponse<AdminQtVideoClipItem>> changeClipStatus(
            Authentication authentication,
            @PathVariable Long clipId,
            @Valid @RequestBody ClipStatusRequest request
    ) {
        Long adminUserId = requireManager(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                adminQtVideoService.changeClipStatus(adminUserId, clipId, request.status())));
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
