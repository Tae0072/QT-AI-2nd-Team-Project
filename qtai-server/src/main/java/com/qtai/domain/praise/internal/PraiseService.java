package com.qtai.domain.praise.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.SaveMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 찬양 도메인 진입점.
 *
 * 운영 정책 (v3.1):
 *   - 큐레이션 등록은 ADMIN만 (컨트롤러 @PreAuthorize)
 *   - AI 기반 찬양 자동 추천/생성 금지
 *   - 가사·음원 본문 저장 금지 — 메타정보만 보관
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PraiseService implements
        CreatePraiseUseCase, ListPraiseUseCase,
        SaveMemberPraiseSongUseCase, ListMemberPraiseSongUseCase {

    private final PraiseSongRepository praiseSongRepository;
    private final MemberPraiseSongRepository memberPraiseSongRepository;

    // ── CreatePraiseUseCase (ADMIN) ──

    @Override
    @Transactional
    public PraiseResponse create(PraiseCreateRequest request) {
        PraiseSong song = PraiseSong.builder()
                .title(request.title())
                .artist(request.artist())
                .sourceType("CURATED")
                .licenseNote(request.licenseNote())
                .status("ACTIVE")
                .build();
        praiseSongRepository.save(song);
        return PraiseResponse.from(song);
    }

    // ── ListPraiseUseCase ──

    @Override
    public Page<PraiseResponse> listActive(Pageable pageable) {
        return praiseSongRepository.findByStatus("ACTIVE", pageable)
                .map(PraiseResponse::from);
    }

    // ── SaveMemberPraiseSongUseCase ──

    @Override
    @Transactional
    public MemberPraiseSongResponse save(Long memberId, MemberPraiseSongCreateRequest request) {
        // 큐레이션 곡 중복 체크
        if (request.praiseSongId() != null) {
            if (memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(
                    memberId, request.praiseSongId())) {
                throw new BusinessException(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
            }
            // 큐레이션 곡 존재 확인
            PraiseSong song = praiseSongRepository.findById(request.praiseSongId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRAISE_SONG_NOT_FOUND));

            MemberPraiseSong mps = MemberPraiseSong.builder()
                    .memberId(memberId)
                    .praiseSongId(request.praiseSongId())
                    .deviceSongKey(request.deviceSongKey())
                    .displayTitle(request.displayTitle())
                    .build();
            memberPraiseSongRepository.save(mps);

            return MemberPraiseSongResponse.of(mps,
                    song.getTitle(), song.getArtist(), song.getSourceType());
        }

        // 디바이스 전용 항목
        MemberPraiseSong mps = MemberPraiseSong.builder()
                .memberId(memberId)
                .deviceSongKey(request.deviceSongKey())
                .displayTitle(request.displayTitle())
                .build();
        memberPraiseSongRepository.save(mps);

        return MemberPraiseSongResponse.fromDevice(mps);
    }

    @Override
    @Transactional
    public void remove(Long memberId, Long memberPraiseSongId) {
        MemberPraiseSong mps = memberPraiseSongRepository
                .findByIdAndMemberId(memberPraiseSongId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRAISE_SONG_SAVE_NOT_FOUND));
        memberPraiseSongRepository.delete(mps);
    }

    // ── ListMemberPraiseSongUseCase ──

    @Override
    public Page<MemberPraiseSongResponse> listMy(Long memberId, Pageable pageable) {
        return memberPraiseSongRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(mps -> {
                    if (mps.getPraiseSongId() != null) {
                        return praiseSongRepository.findById(mps.getPraiseSongId())
                                .map(song -> MemberPraiseSongResponse.of(mps,
                                        song.getTitle(), song.getArtist(), song.getSourceType()))
                                .orElse(MemberPraiseSongResponse.fromDevice(mps));
                    }
                    return MemberPraiseSongResponse.fromDevice(mps);
                });
    }

    @Override
    public long countMy(Long memberId) {
        return memberPraiseSongRepository.countByMemberId(memberId);
    }
}
