package com.qtai.domain.qt.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.qt.api.admin.CreateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.HideAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.ListAdminQtPassagesUseCase;
import com.qtai.domain.qt.api.admin.PublishAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.UpdateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageCommand;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;
import com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/qt-passages")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQtPassageController {

    private static final List<String> QT_PASSAGE_ADMIN_ROLES = List.of("OPERATOR", "SUPER_ADMIN");

    private final ListAdminQtPassagesUseCase listUseCase;
    private final CreateAdminQtPassageUseCase createUseCase;
    private final UpdateAdminQtPassageUseCase updateUseCase;
    private final PublishAdminQtPassageUseCase publishUseCase;
    private final HideAdminQtPassageUseCase hideUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    public AdminQtPassageController(
            ListAdminQtPassagesUseCase listUseCase,
            CreateAdminQtPassageUseCase createUseCase,
            UpdateAdminQtPassageUseCase updateUseCase,
            PublishAdminQtPassageUseCase publishUseCase,
            HideAdminQtPassageUseCase hideUseCase,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase
    ) {
        this.listUseCase = listUseCase;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.publishUseCase = publishUseCase;
        this.hideUseCase = hideUseCase;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminQtPassageListResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireOperator(authentication);
        AdminQtPassageListResponse response = listUseCase.list(
                new ListAdminQtPassagesQuery(status, from, to, q, page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminQtPassageResponse>> create(
            Authentication authentication,
            @Valid @RequestBody AdminQtPassageRequest request
    ) {
        AdminUserInfo admin = requireOperator(authentication);
        AdminQtPassageResponse response = createUseCase.create(toCommand(admin.adminUserId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{qtPassageId}")
    public ResponseEntity<ApiResponse<AdminQtPassageResponse>> update(
            @PathVariable("qtPassageId") Long qtPassageId,
            Authentication authentication,
            @Valid @RequestBody AdminQtPassageRequest request
    ) {
        AdminUserInfo admin = requireOperator(authentication);
        AdminQtPassageResponse response = updateUseCase.update(qtPassageId, toCommand(admin.adminUserId(), request));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{qtPassageId}/publish")
    public ResponseEntity<ApiResponse<AdminQtPassageResponse>> publish(
            @PathVariable("qtPassageId") Long qtPassageId,
            Authentication authentication
    ) {
        AdminUserInfo admin = requireOperator(authentication);
        AdminQtPassageResponse response = publishUseCase.publish(admin.adminUserId(), qtPassageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{qtPassageId}/hide")
    public ResponseEntity<ApiResponse<AdminQtPassageResponse>> hide(
            @PathVariable("qtPassageId") Long qtPassageId,
            Authentication authentication
    ) {
        AdminUserInfo admin = requireOperator(authentication);
        AdminQtPassageResponse response = hideUseCase.hide(admin.adminUserId(), qtPassageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private AdminUserInfo requireOperator(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long memberId = resolvePrincipalId(authentication);
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, QT_PASSAGE_ADMIN_ROLES);
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

    private static AdminQtPassageCommand toCommand(Long adminId, AdminQtPassageRequest request) {
        // endChapter는 선택 필드 — 미입력(null) 시 단일 장으로 간주해 시작 장으로 보정한다.
        Short endChapter = request.endChapter() == null ? request.chapter() : request.endChapter();
        return new AdminQtPassageCommand(
                adminId,
                request.qtDate(),
                request.bookId(),
                request.chapter(),
                endChapter,
                request.startVerse(),
                request.endVerse(),
                request.title(),
                request.mainVerseRef()
        );
    }

    public record AdminQtPassageRequest(
            @NotNull LocalDate qtDate,
            @NotNull @Min(1) @Max(66) Short bookId,
            @NotNull @Min(1) Short chapter,
            @Min(1) Short endChapter,
            @NotNull @Min(1) Short startVerse,
            @NotNull @Min(1) Short endVerse,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 100) String mainVerseRef
    ) {
    }
}
