package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.member.api.GetMemberDetailForAdminUseCase;
import com.qtai.domain.member.api.ListMembersForAdminUseCase;
import com.qtai.domain.member.api.UpdateMemberStatusForAdminUseCase;
import com.qtai.domain.member.api.dto.AdminMemberDetailResponse;
import com.qtai.domain.member.api.dto.AdminMemberResponse;
import com.qtai.domain.member.api.dto.MemberStatusUpdateRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원 관리 API (F-04·F-10).
 *
 * <p>목록·검색·상세(닉네임 변경 시각·신고/나눔 집계)·정지/정지해제. 개인정보(email·kakaoId)는 미노출.
 */
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final ListMembersForAdminUseCase listMembersForAdminUseCase;
    private final UpdateMemberStatusForAdminUseCase updateMemberStatusForAdminUseCase;
    private final GetMemberDetailForAdminUseCase getMemberDetailForAdminUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /** GET /api/v1/admin/members?status=&q=&page=&size= */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminMemberResponse>>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                ApiResponse.success(listMembersForAdminUseCase.listForAdmin(status, q, pageable)));
    }

    /** GET /api/v1/admin/members/{memberId} */
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> detail(
            @PathVariable Long memberId,
            Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(listMembersForAdminUseCase.getForAdmin(memberId)));
    }

    /** GET /api/v1/admin/members/{memberId}/detail — 닉네임 변경 시각·신고/나눔 집계 포함 상세 */
    @GetMapping("/{memberId}/detail")
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> detailFull(
            @PathVariable Long memberId,
            Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(getMemberDetailForAdminUseCase.getDetailForAdmin(memberId)));
    }

    /** PATCH /api/v1/admin/members/{memberId}/status */
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> updateStatus(
            @PathVariable Long memberId,
            Authentication authentication,
            @Valid @RequestBody MemberStatusUpdateRequest request) {
        requireOperator(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(updateMemberStatusForAdminUseCase.updateStatus(memberId, request)));
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
