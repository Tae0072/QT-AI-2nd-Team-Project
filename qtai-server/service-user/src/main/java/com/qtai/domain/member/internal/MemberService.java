package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.ChangeNicknameUseCase;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.GetProfilePhotoUseCase;
import com.qtai.domain.member.api.UpdateProfilePhotoUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfilePhotoView;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
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
public class MemberService implements GetMemberUseCase, UpdateProfileUseCase, WithdrawUseCase, ChangeNicknameUseCase,
        UpdateProfilePhotoUseCase, GetProfilePhotoUseCase {

    // 허용 이미지 형식·최대 크기(프로필 사진).
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024; // 5MB

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

    /**
     * 활성 회원 공개 프로필 일괄 조회 — 목록 N+1 방지용.
     *
     * <p>단건 {@link #getMemberPublic}과 달리 탈퇴·정지 회원은 예외 없이
     * 결과에서 제외한다. 호출자(댓글 목록 등)는 누락 id를 자체 표시 정책으로
     * 폴백한다 — 탈퇴 회원 1명이 목록 API 전체를 404로 깨뜨리던 결함의 수정 계약.
     */
    @Override
    public java.util.List<MemberPublicResponse> getActivePublicProfiles(java.util.Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return java.util.List.of();
        }
        return memberRepository.findAllById(new java.util.LinkedHashSet<>(memberIds)).stream()
                .filter(Member::isActive)
                .map(member -> new MemberPublicResponse(
                        member.getId(),
                        member.getNickname(),
                        member.getProfileImageUrl()
                ))
                .toList();
    }

    /** 멘션 자동완성 기본/최대 결과 수. */
    private static final int MENTION_SEARCH_DEFAULT_LIMIT = 8;
    private static final int MENTION_SEARCH_MAX_LIMIT = 20;

    @Override
    public java.util.List<MemberPublicResponse> resolveActiveByNicknames(java.util.Collection<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            return java.util.List.of();
        }
        return memberRepository.findByNicknameIn(new java.util.LinkedHashSet<>(nicknames)).stream()
                .filter(Member::isActive)
                .map(member -> new MemberPublicResponse(
                        member.getId(), member.getNickname(), member.getProfileImageUrl()))
                .toList();
    }

    @Override
    public java.util.List<MemberPublicResponse> searchActiveByNicknamePrefix(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return java.util.List.of();
        }
        int capped = (limit < 1) ? MENTION_SEARCH_DEFAULT_LIMIT : Math.min(limit, MENTION_SEARCH_MAX_LIMIT);
        return memberRepository
                .findByNicknameStartingWithIgnoreCaseOrderByNicknameAsc(
                        prefix.trim(), org.springframework.data.domain.PageRequest.of(0, capped))
                .stream()
                .filter(Member::isActive)
                .map(member -> new MemberPublicResponse(
                        member.getId(), member.getNickname(), member.getProfileImageUrl()))
                .toList();
    }

    // ── UpdateProfileUseCase ──

    @Override
    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = findActiveMemberOrThrow(memberId);

        if (request.nickname() != null) {
            // trim/공백 검증은 changeNicknameInternal로 일원화(P2) — changeNickname 경로와 동일 규칙 적용.
            changeNicknameInternal(member, request.nickname());
        }
        if (request.profileImageUrl() != null) {
            member.updateProfileImageUrl(request.profileImageUrl());
        }

        return toResponse(member);
    }

    // ── UpdateProfilePhotoUseCase / GetProfilePhotoUseCase (프로필 사진 업로드·조회) ──

    @Override
    @Transactional
    public MemberResponse updateProfilePhoto(Long memberId, byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일이 비어 있습니다.");
        }
        if (data.length > MAX_PHOTO_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지는 5MB 이하만 가능합니다.");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 이미지 형식입니다(jpeg/png/webp).");
        }
        Member member = findActiveMemberOrThrow(memberId);
        member.updateProfilePhoto(data, normalized, clock);
        return toResponse(member);
    }

    @Override
    @Transactional
    public MemberResponse deleteProfilePhoto(Long memberId) {
        Member member = findActiveMemberOrThrow(memberId);
        member.clearProfilePhoto();
        return toResponse(member);
    }

    @Override
    public ProfilePhotoView getOwnProfilePhoto(Long memberId) {
        Member member = findActiveMemberOrThrow(memberId);
        if (!member.hasProfilePhoto()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "프로필 사진이 없습니다.");
        }
        return new ProfilePhotoView(member.getProfileImageData(), member.getProfileImageContentType());
    }

    // ── ChangeNicknameUseCase (닉네임 변경 — 즉시 변경 가능, 2026-06-11 잠금 폐지) ──

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

    /** 시스템이 부여하는 임시 닉네임 접두사 — 사용자 닉네임으로는 예약(사칭 방지). */
    private static final String RESERVED_NICKNAME_PREFIX = "user_";

    private void changeNicknameInternal(Member member, String rawNickname) {
        // 두 진입점(updateProfile / changeNickname) 공통: 앞뒤 공백 제거 후 공백-only 거부(P2 trim 정책 일원화).
        String newNickname = rawNickname == null ? null : rawNickname.trim();
        if (newNickname == null || newNickname.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 공백일 수 없습니다.");
        }
        // 7일 잠금 검사 제거(2026-06-11 피드백) — 닉네임은 즉시 변경 가능. Member.isNicknameChangeable 참조.
        // 임시 닉네임 접두사(user_) 사칭 차단 — 시스템 예약 접두사
        if (newNickname.startsWith(RESERVED_NICKNAME_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "'" + RESERVED_NICKNAME_PREFIX + "'로 시작하는 닉네임은 사용할 수 없습니다.");
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
