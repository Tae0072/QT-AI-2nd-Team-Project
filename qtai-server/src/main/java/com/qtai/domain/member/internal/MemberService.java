package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.ChangeNicknameUseCase;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * 회원 도메인 서비스. Phase 3(mypage-api) 범위.
 *
 * <p>Login/Logout/JWT 는 auth-jwt 브랜치에서 구현 예정.
 * <p>도메인 경계 정책: Entity → DTO 변환은 이 서비스에서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements GetMemberUseCase, UpdateProfileUseCase, WithdrawUseCase, ChangeNicknameUseCase {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ── GetMemberUseCase ──

    @Override
    public MemberResponse getMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        return toResponse(member);
    }

    @Override
    public MemberPublicResponse getMemberPublic(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        // 탈퇴 회원은 공개 프로필을 노출하지 않는다
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return new MemberPublicResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl()
        );
    }

    // ── UpdateProfileUseCase ──

    @Override
    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = findActiveMemberOrThrow(memberId);

        if (request.nickname() != null) {
            String trimmed = request.nickname().trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 공백일 수 없습니다.");
            }
            changeNicknameInternal(member, trimmed);
        }
        if (request.profileImageUrl() != null) {
            member.updateProfileImageUrl(request.profileImageUrl());
        }

        return toResponse(member);
    }

    // ── ChangeNicknameUseCase (닉네임 변경, 7일 잠금) ──

    @Override
    @Transactional
    public MemberResponse changeNickname(Long memberId, NicknameChangeRequest request) {
        Member member = findActiveMemberOrThrow(memberId);
        changeNicknameInternal(member, request.nickname());
        return toResponse(member);
    }

    /**
     * 닉네임 사용가능 여부 확인 (true = 사용 가능).
     */
    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    // ── WithdrawUseCase ──

    @Override
    @Transactional
    public void withdraw(Long memberId, String reason) {
        Member member = findActiveMemberOrThrow(memberId);
        member.withdraw(clock);
        // 세션(refresh token) 무효화는 AFTER_COMMIT 이벤트로 분리 —
        // 트랜잭션 롤백 시 토큰 유지, Redis 실패가 탈퇴를 깨지 않음
        // (MemberWithdrawnEventHandler 참조)
        eventPublisher.publishEvent(
                new MemberWithdrawnEvent(UUID.randomUUID().toString(), memberId));
        // TODO: reason 은 감사(audit) 전용 채널로 분리 — 일반 로그에 개인정보 포함 방지
        log.info("회원 탈퇴: memberId={}", memberId);
        // AuditLog 연동은 audit 도메인 구현 후 추가 예정 (reason 포함)
    }

    // ── private helpers ──

    private void changeNicknameInternal(Member member, String newNickname) {
        if (!member.isNicknameChangeable(clock)) {
            throw new BusinessException(ErrorCode.NICKNAME_LOCKED,
                    "닉네임은 " + member.getNicknameUnlockAt() + " 이후에 변경할 수 있습니다.");
        }
        if (memberRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        try {
            member.changeNickname(newNickname, clock);
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // TOCTOU: existsByNickname 이후 동시 INSERT → UK 위반 시 비즈니스 예외로 변환
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
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

    /** Entity → DTO 변환 (api/dto 가 internal 을 import 하지 않기 위한 패턴). */
    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImageUrl(),
                member.getStatus().name(),
                member.getRole().name(),
                member.getNicknameUnlockAt(),
                member.getCreatedAt()
        );
    }
}
