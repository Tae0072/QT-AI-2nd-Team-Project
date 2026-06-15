package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.member.api.GetMemberDetailForAdminUseCase;
import com.qtai.domain.member.api.ListMembersForAdminUseCase;
import com.qtai.domain.member.api.ListNicknameHistoryForAdminUseCase;
import com.qtai.domain.member.api.UpdateMemberStatusForAdminUseCase;
import com.qtai.domain.member.api.dto.AdminMemberDetailResponse;
import com.qtai.domain.member.api.dto.AdminMemberResponse;
import com.qtai.domain.member.api.dto.MemberStatusUpdateRequest;
import com.qtai.domain.member.api.dto.NicknameHistoryItem;
import com.qtai.domain.mission.api.GetMemberMissionProgressUseCase;
import com.qtai.domain.mission.api.dto.MissionProgressResponse;
import com.qtai.domain.note.api.ListMemberNotesForAdminUseCase;
import com.qtai.domain.note.api.dto.AdminNoteItem;
import com.qtai.domain.sharing.api.AdminMemberSharingQueryUseCase;
import com.qtai.domain.sharing.api.dto.AdminMemberCommentItem;
import com.qtai.domain.sharing.api.dto.AdminMemberLikedPostItem;
import com.qtai.domain.sharing.api.dto.AdminMemberPostItem;
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
    private final ListMemberNotesForAdminUseCase listMemberNotesForAdminUseCase;
    private final AdminMemberSharingQueryUseCase adminMemberSharingQueryUseCase;
    private final GetMemberMissionProgressUseCase getMemberMissionProgressUseCase;
    private final ListNicknameHistoryForAdminUseCase listNicknameHistoryForAdminUseCase;

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

    /** GET /api/v1/admin/members/{memberId}/notes — 회원이 작성한 노트(메타데이터, 최신순) */
    @GetMapping("/{memberId}/notes")
    public ResponseEntity<ApiResponse<Page<AdminNoteItem>>> notes(
            @PathVariable Long memberId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                listMemberNotesForAdminUseCase.listNotesByMember(memberId, pageable)));
    }

    /** GET /api/v1/admin/members/{memberId}/posts — 회원이 공유한 나눔글(전체 상태, 최신순) */
    @GetMapping("/{memberId}/posts")
    public ResponseEntity<ApiResponse<Page<AdminMemberPostItem>>> posts(
            @PathVariable Long memberId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberSharingQueryUseCase.listPostsByMember(memberId, PageRequest.of(page, size))));
    }

    /** GET /api/v1/admin/members/{memberId}/comments — 회원이 작성한 댓글(삭제 포함, 최신순) */
    @GetMapping("/{memberId}/comments")
    public ResponseEntity<ApiResponse<Page<AdminMemberCommentItem>>> comments(
            @PathVariable Long memberId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberSharingQueryUseCase.listCommentsByMember(memberId, PageRequest.of(page, size))));
    }

    /** GET /api/v1/admin/members/{memberId}/likes — 회원이 좋아요한 나눔글(최신순) */
    @GetMapping("/{memberId}/likes")
    public ResponseEntity<ApiResponse<Page<AdminMemberLikedPostItem>>> likes(
            @PathVariable Long memberId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberSharingQueryUseCase.listLikedPostsByMember(memberId, PageRequest.of(page, size))));
    }

    /** GET /api/v1/admin/members/{memberId}/missions — 회원 미션 진행률 */
    @GetMapping("/{memberId}/missions")
    public ResponseEntity<ApiResponse<List<MissionProgressResponse>>> missions(
            @PathVariable Long memberId,
            Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                getMemberMissionProgressUseCase.getMissionProgress(memberId)));
    }

    /** GET /api/v1/admin/members/{memberId}/nickname-history — 닉네임 변경 이력(최신순) */
    @GetMapping("/{memberId}/nickname-history")
    public ResponseEntity<ApiResponse<Page<NicknameHistoryItem>>> nicknameHistory(
            @PathVariable Long memberId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                listNicknameHistoryForAdminUseCase.listNicknameHistory(memberId, PageRequest.of(page, size))));
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
