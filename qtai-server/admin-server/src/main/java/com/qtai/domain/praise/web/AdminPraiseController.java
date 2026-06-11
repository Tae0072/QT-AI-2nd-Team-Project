package com.qtai.domain.praise.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.DeletePraiseUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.UpdatePraiseUseCase;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import com.qtai.domain.praise.api.dto.PraiseUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 찬양 큐레이션 API.
 *
 * <p>F-06·F-09: 관리자는 큐레이션 곡을 등록·수정·삭제할 수 있다.
 * 숨김(HIDDEN) 상태 전환은 v1 범위 제외.
 */
@RestController
@RequestMapping("/api/v1/admin/praise-songs")
@RequiredArgsConstructor
public class AdminPraiseController {

    private final ListPraiseUseCase listPraiseUseCase;
    private final CreatePraiseUseCase createPraiseUseCase;
    private final UpdatePraiseUseCase updatePraiseUseCase;
    private final DeletePraiseUseCase deletePraiseUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /** GET /api/v1/admin/praise-songs?status=ACTIVE&page=0&size=20 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PraiseResponse>>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(listPraiseUseCase.listAdmin(status, pageable)));
    }

    /** POST /api/v1/admin/praise-songs */
    @PostMapping
    public ResponseEntity<ApiResponse<PraiseResponse>> create(
            Authentication authentication,
            @Valid @RequestBody PraiseCreateRequest request) {
        Long adminId = requireOperator(authentication);
        PraiseResponse response = createPraiseUseCase.create(adminId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/praise-songs/" + response.id()))
                .body(ApiResponse.success(response));
    }

    /** PATCH /api/v1/admin/praise-songs/{id} */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PraiseResponse>> update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody PraiseUpdateRequest request) {
        Long adminId = requireOperator(authentication);
        PraiseResponse response = updatePraiseUseCase.update(adminId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** DELETE /api/v1/admin/praise-songs/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = requireOperator(authentication);
        deletePraiseUseCase.delete(adminId, id);
        return ResponseEntity.noContent().build();
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
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, List.of("OPERATOR")).adminUserId();
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
