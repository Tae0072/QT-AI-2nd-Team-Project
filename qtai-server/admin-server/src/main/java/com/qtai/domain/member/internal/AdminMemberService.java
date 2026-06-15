package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberDetailForAdminUseCase;
import com.qtai.domain.member.api.ListMembersForAdminUseCase;
import com.qtai.domain.member.api.UpdateMemberStatusForAdminUseCase;
import com.qtai.domain.member.api.dto.AdminMemberDetailResponse;
import com.qtai.domain.member.api.dto.AdminMemberResponse;
import com.qtai.domain.member.api.dto.MemberStatusUpdateRequest;
import com.qtai.domain.report.api.MemberReportStatsUseCase;
import com.qtai.domain.sharing.api.MemberSharingStatsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 운영 서비스 (F-04/F-10). admin-server 고유 기능(사용자용 MemberService와 분리).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService implements ListMembersForAdminUseCase, UpdateMemberStatusForAdminUseCase,
        GetMemberDetailForAdminUseCase {

    private final MemberRepository memberRepository;
    private final MemberSharingStatsUseCase memberSharingStatsUseCase;
    private final MemberReportStatsUseCase memberReportStatsUseCase;

    @Override
    public Page<AdminMemberResponse> listForAdmin(String status, String q, Pageable pageable) {
        MemberStatus statusFilter = parseStatusFilter(status);
        String keyword = (q == null || q.isBlank()) ? null : escapeLike(q.trim());
        return memberRepository.searchForAdmin(statusFilter, keyword, pageable).map(this::toResponse);
    }

    @Override
    public AdminMemberResponse getForAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return toResponse(member);
    }

    @Override
    @Transactional
    public AdminMemberResponse updateStatus(Long memberId, MemberStatusUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        MemberStatus target = MemberStatus.valueOf(request.status());
        try {
            if (target == MemberStatus.SUSPENDED) {
                member.suspendByAdmin();
            } else {
                member.activateByAdmin();
            }
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        log.info("관리자 회원 상태 변경. memberId={}, status={}", memberId, target);
        return toResponse(member);
    }

    @Override
    public AdminMemberDetailResponse getDetailForAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> postIds = memberSharingStatsUseCase.listPostIdsByMember(memberId);
        List<Long> commentIds = memberSharingStatsUseCase.listCommentIdsByMember(memberId);
        long sharingPostCount = memberSharingStatsUseCase.countPostsByMember(memberId);
        long reportsFiled = memberReportStatsUseCase.countFiledBy(memberId);
        long reportsReceived = memberReportStatsUseCase.countReceivedForTargets("POST", postIds)
                + memberReportStatsUseCase.countReceivedForTargets("COMMENT", commentIds);

        return new AdminMemberDetailResponse(
                member.getId(),
                member.getNickname(),
                member.getStatus().name(),
                member.getRole().name(),
                member.getNicknameChangedAt(),
                member.getWithdrawnAt(),
                member.getCreatedAt(),
                sharingPostCount,
                reportsFiled,
                reportsReceived
        );
    }

    private MemberStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MemberStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private AdminMemberResponse toResponse(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getNickname(),
                member.getStatus().name(),
                member.getRole().name(),
                member.getNicknameChangedAt(),
                member.getWithdrawnAt(),
                member.getCreatedAt()
        );
    }
}
