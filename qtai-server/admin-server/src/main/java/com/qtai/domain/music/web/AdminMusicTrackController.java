package com.qtai.domain.music.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.music.api.CreateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.HideAdminMusicTrackUseCase;
import com.qtai.domain.music.api.ListAdminMusicTrackUseCase;
import com.qtai.domain.music.api.PublishAdminMusicTrackUseCase;
import com.qtai.domain.music.api.UpdateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.dto.AdminMusicTrackCommand;
import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/music-tracks")
@RequiredArgsConstructor
public class AdminMusicTrackController {

    private static final List<String> MUSIC_TRACK_ADMIN_ROLES = List.of("OPERATOR", "SUPER_ADMIN");
    private static final long MAX_AUDIO_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_AUDIO_MIME_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/ogg",
            "audio/wav",
            "audio/x-wav",
            "audio/webm",
            "audio/flac"
    );

    private final ListAdminMusicTrackUseCase listAdminMusicTrackUseCase;
    private final CreateAdminMusicTrackUseCase createAdminMusicTrackUseCase;
    private final UpdateAdminMusicTrackUseCase updateAdminMusicTrackUseCase;
    private final PublishAdminMusicTrackUseCase publishAdminMusicTrackUseCase;
    private final HideAdminMusicTrackUseCase hideAdminMusicTrackUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminMusicTrackListResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(listAdminMusicTrackUseCase.listAdmin(status, pageable)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> create(
            Authentication authentication,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "BGM") String category,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) Integer durationSec,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            @RequestParam(required = false) String licenseNote,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Long adminUserId = requireOperator(authentication);
        AdminMusicTrackResponse response = createAdminMusicTrackUseCase.createAdmin(
                adminUserId,
                toCommand(title, category, mimeType, durationSec, sortOrder, licenseNote, file, true)
        );
        return ResponseEntity.created(URI.create("/api/v1/admin/music-tracks/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> update(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) Integer durationSec,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) String licenseNote,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Long adminUserId = requireOperator(authentication);
        AdminMusicTrackResponse response = updateAdminMusicTrackUseCase.updateAdmin(
                adminUserId,
                id,
                toCommand(title, category, mimeType, durationSec, sortOrder, licenseNote, file, false)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> publish(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminUserId = requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(publishAdminMusicTrackUseCase.publishAdmin(adminUserId, id)));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> hide(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminUserId = requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(hideAdminMusicTrackUseCase.hideAdmin(adminUserId, id)));
    }

    private static AdminMusicTrackCommand toCommand(
            String title,
            String category,
            String mimeType,
            Integer durationSec,
            Integer sortOrder,
            String licenseNote,
            MultipartFile file,
            boolean requireFile) {
        byte[] audioData = null;
        if (file != null && !file.isEmpty()) {
            validateAudioFileSize(file);
            try {
                audioData = file.getBytes();
            } catch (IOException exception) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "음원 파일을 읽을 수 없습니다.");
            }
        } else if (requireFile) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "음원 파일은 필수입니다.");
        }
        return new AdminMusicTrackCommand(
                title,
                category,
                resolveMimeType(mimeType, file, requireFile),
                durationSec,
                sortOrder,
                licenseNote,
                audioData
        );
    }

    private static String resolveMimeType(String requestedMimeType, MultipartFile file, boolean requireFile) {
        if (requestedMimeType != null && !requestedMimeType.isBlank()) {
            return validateMimeType(requestedMimeType.trim());
        }
        if (file != null && file.getContentType() != null && !file.getContentType().isBlank()) {
            return validateMimeType(file.getContentType().trim());
        }
        return requireFile || (file != null && !file.isEmpty()) ? "audio/mpeg" : null;
    }

    private static String validateMimeType(String mimeType) {
        String normalized = mimeType.toLowerCase();
        if (!ALLOWED_AUDIO_MIME_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "unsupported audio mime type: " + mimeType);
        }
        return normalized;
    }

    private static void validateAudioFileSize(MultipartFile file) {
        if (file.getSize() > MAX_AUDIO_FILE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "audio file size must be 10 MiB or less.");
        }
    }

    private Long requireOperator(Authentication requestAuthentication) {
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
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, MUSIC_TRACK_ADMIN_ROLES).adminUserId();
    }

    private static Long resolvePrincipalId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(principal));
        } catch (NumberFormatException exception) {
            try {
                return Long.valueOf(authentication.getName());
            } catch (NumberFormatException nested) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
    }
}
