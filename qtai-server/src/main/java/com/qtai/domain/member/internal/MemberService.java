package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 도메인 진입점 — Phase 3(마이페이지) 범위.
 *
 * Login/Logout/JWT는 auth-jwt 브랜치에서 구현 예정.
 * 이 브랜치에서는 조회·프로필·닉네임·탈퇴만 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements GetMemberUseCase, UpdateProfileUseCase, WithdrawUseCase {

    private final MemberRepository memberRepository;

    // ── GetMemberUseCase ──

    @Override
    public MemberResponse getMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        return MemberResponse.from(member);
    }

    // ── UpdateProfileUseCase ──

    @Override
    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = findActiveMemberOrThrow(memberId);

        if (request.nickname() != null) {
            changeNickname(member, request.nickname());
        }
        if (request.profileImageUrl() != null) {
            member.updateProfileImageUrl(request.profileImageUrl());
        }

        return MemberResponse.from(member);
    }

    // ── 닉네임 변경 (7일 잠금) ──

    @Transactional
    public MemberResponse changeNickname(Long memberId, NicknameChangeRequest request) {
        Member member = findActiveMemberOrThrow(memberId);
        changeNickname(member, request.nickname());
        return MemberResponse.from(member);
    }

    /**
     * 닉네임 중복 확인 (true = 사용 가능).
     */
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    // ── WithdrawUseCase ──

    @Override
    @Transactional
    public void withdraw(Long memberId, String reason) {
        Member member = findActiveMemberOrThrow(memberId);
        member.withdraw();
        // AuditLog 기록은 audit 도메인 연동 시 추가
    }

    // ── private helpers ──

    private void changeNickname(Member member, String newNickname) {
        if (!member.isNicknameChangeable()) {
            throw new BusinessException(ErrorCode.NICKNAME_CHANGE_LOCKED,
                    "닉네임은 " + member.getNicknameUnlockAt() + " 이후에 변경할 수 있습니다.");
        }
        if (memberRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        member.changeNickname(newNickname);
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Member findActiveMemberOrThrow(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }
        return member;
    }
}
