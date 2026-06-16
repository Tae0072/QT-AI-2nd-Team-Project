package com.qtai.domain.praise.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.DeletePraiseUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.SaveMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.UpdatePraiseUseCase;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import com.qtai.domain.praise.api.dto.PraiseUpdateRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 찬양 도메인 서비스.
 *
 * <p>설계 결정 (v3.1):
 * <ul>
 *   <li>큐레이션 곡 등록은 ADMIN 전용 (컨트롤러에서 @PreAuthorize)</li>
 *   <li>도메인 경계 정책: Entity → DTO 변환은 이 서비스에서 수행한다</li>
 * </ul>
 *
 * <p><b>service-bible 동기화 계획 (CLAUDE.md §1 admin-server-sync-rules):</b>
 * {@code create}/{@code listActive}/{@code save}/{@code listMy} 등 사용자 노출
 * 도메인 로직의 원본(SSoT)은 service-bible의 동명 {@code PraiseService}이며 admin-server는
 * 이를 따라간다(규칙 ①). 반면 {@code update}/{@code delete}/{@code listAdmin}은
 * 관리자 콘솔(AD-05) 전용 운영 기능으로, 사용자 앱에는 큐레이션 곡 수정·삭제·전체 상태
 * 조회 경로가 없다. 따라서 규칙 ②(admin 고유 기능은 admin-server에서 직접 관리)에 따라
 * 이 세 메서드는 admin-server에만 둔다. service-bible의 {@code PraiseService}는 이 세
 * 메서드를 의도적으로 포함하지 않는다(동기화 대상 아님). 향후 사용자 앱에 큐레이션
 * 변경 경로가 생기면 그때 service-bible을 원본으로 승격한 뒤 admin-server가 따라간다.
 *
 * <p><b>삭제 정책 (delete):</b> 큐레이션 곡 물리 삭제 시 이를 저장한 회원의
 * {@code member_praise_songs} 참조 행({@code fk_mps_praise_song}, RESTRICT)을 먼저
 * 같은 트랜잭션 안에서 정리한다(cascade). 회원 저장 행은 곡을 가리키는 순수 참조
 * 메타데이터(사용자 작성 콘텐츠 아님, F-09: 서버는 메타데이터만 저장)이므로 곡과 함께
 * 정리해도 사용자 콘텐츠 손실이 아니며, 04 §4.7.6의 {@code 204 No Content} 계약을 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PraiseService implements
        CreatePraiseUseCase, ListPraiseUseCase,
        UpdatePraiseUseCase, DeletePraiseUseCase,
        SaveMemberPraiseSongUseCase, ListMemberPraiseSongUseCase {

    private final PraiseSongRepository praiseSongRepository;
    private final MemberPraiseSongRepository memberPraiseSongRepository;
    private final Clock clock;

    // ── CreatePraiseUseCase (ADMIN) ──

    @Override
    @Transactional
    public PraiseResponse create(Long adminId, PraiseCreateRequest request) {
        PraiseSong song = PraiseSong.builder()
                .title(request.title())
                .artist(request.artist())
                .sourceType(PraiseSourceType.CURATED)
                .licenseNote(request.licenseNote())
                .status(parseStatusOrDefault(request.status()))
                .build();
        praiseSongRepository.save(song);
        log.info("큐레이션 곡 등록: adminId={}, songId={}, title={}", adminId, song.getId(), song.getTitle());
        return toResponse(song);
    }

    // ── ListPraiseUseCase ──

    @Override
    public Page<PraiseResponse> listActive(Pageable pageable) {
        return praiseSongRepository.findByStatus(PraiseSongStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    private PraiseSongStatus parseStatusOrDefault(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return PraiseSongStatus.ACTIVE;
        }
        try {
            return PraiseSongStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 찬양 곡 상태입니다: " + rawStatus);
        }
    }

    @Override
    public Page<PraiseResponse> listAdmin(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            PraiseSongStatus statusEnum;
            try {
                statusEnum = PraiseSongStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // 정의되지 않은 status 값은 400으로 매핑한다 (valueOf의 500 방지).
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "유효하지 않은 status 값입니다: " + status);
            }
            return praiseSongRepository.findByStatus(statusEnum, pageable).map(this::toResponse);
        }
        return praiseSongRepository.findAll(pageable).map(this::toResponse);
    }

    // ── UpdatePraiseUseCase (ADMIN) ──

    @Override
    @Transactional
    public PraiseResponse update(Long adminId, Long praiseSongId, PraiseUpdateRequest request) {
        PraiseSong song = praiseSongRepository.findById(praiseSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRAISE_SONG_NOT_FOUND));
        song.update(request.title(), request.artist(), request.licenseNote(),
                parseStatusOrNull(request.status()));
        log.info("큐레이션 곡 수정: adminId={}, songId={}, title={}", adminId, song.getId(), song.getTitle());
        return toResponse(song);
    }

    private PraiseSongStatus parseStatusOrNull(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        return parseStatusOrDefault(rawStatus);
    }

    // ── DeletePraiseUseCase (ADMIN) ──

    @Override
    @Transactional
    public void delete(Long adminId, Long praiseSongId) {
        PraiseSong song = praiseSongRepository.findById(praiseSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRAISE_SONG_NOT_FOUND));
        // fk_mps_praise_song(RESTRICT) 위반 방지 — 곡을 저장한 회원 참조 행을 먼저 정리한다.
        long removedRefs = memberPraiseSongRepository.deleteByPraiseSongId(praiseSongId);
        praiseSongRepository.delete(song);
        log.info("큐레이션 곡 삭제: adminId={}, songId={}, 정리된 회원 저장 행={}",
                adminId, praiseSongId, removedRefs);
    }

    // ── SaveMemberPraiseSongUseCase ──

    @Override
    @Transactional
    public MemberPraiseSongResponse save(Long memberId, MemberPraiseSongCreateRequest request) {
        // praiseSongId 와 deviceSongKey 모두 null 이면 저장 대상을 특정할 수 없다.
        if (request.praiseSongId() == null && request.deviceSongKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "praiseSongId 또는 deviceSongKey 중 하나는 필수입니다.");
        }

        // 큐레이션 곡 저장인 경우
        if (request.praiseSongId() != null) {
            if (memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(
                    memberId, request.praiseSongId())) {
                throw new BusinessException(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
            }
            // 큐레이션 곡 존재 검증
            PraiseSong song = praiseSongRepository.findById(request.praiseSongId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRAISE_SONG_NOT_FOUND));

            // 큐레이션 경로에서는 deviceSongKey 를 무시한다 — uk_member_praise_device 충돌 방지.
            MemberPraiseSong mps = MemberPraiseSong.builder()
                    .memberId(memberId)
                    .praiseSongId(request.praiseSongId())
                    .displayTitle(request.displayTitle())
                    .createdAt(LocalDateTime.now(clock))
                    .build();
            try {
                memberPraiseSongRepository.save(mps);
            } catch (DataIntegrityViolationException e) {
                // TOCTOU: existsBy 이후 동시 INSERT → UK 위반 시 비즈니스 예외로 변환
                throw new BusinessException(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
            }

            return toMemberResponse(mps,
                    song.getTitle(), song.getArtist(), song.getSourceType().name());
        }

        // 디바이스 곡 직접 등록 — deviceSongKey 중복 검증
        if (request.deviceSongKey() != null &&
                memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(
                        memberId, request.deviceSongKey())) {
            throw new BusinessException(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
        }

        MemberPraiseSong mps = MemberPraiseSong.builder()
                .memberId(memberId)
                .deviceSongKey(request.deviceSongKey())
                .displayTitle(request.displayTitle())
                .createdAt(LocalDateTime.now(clock))
                .build();
        try {
            memberPraiseSongRepository.save(mps);
        } catch (DataIntegrityViolationException e) {
            // TOCTOU: existsBy 이후 동시 INSERT → UK 위반 시 비즈니스 예외로 변환
            throw new BusinessException(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
        }

        return toMemberDeviceResponse(mps);
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
        Page<MemberPraiseSong> page = memberPraiseSongRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<Long> songIds = page.getContent().stream()
                .map(MemberPraiseSong::getPraiseSongId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, PraiseSong> songMap = praiseSongRepository.findAllById(songIds).stream()
                .collect(Collectors.toMap(PraiseSong::getId, Function.identity()));

        return page.map(mps -> {
            if (mps.getPraiseSongId() != null) {
                PraiseSong song = songMap.get(mps.getPraiseSongId());
                if (song != null) {
                    return toMemberResponse(mps,
                            song.getTitle(), song.getArtist(), song.getSourceType().name());
                }
                // 큐레이션 곡이 삭제된 경우 — sourceType CURATED 유지, title/artist null
                return toMemberResponse(mps, null, null, PraiseSourceType.CURATED.name());
            }
            return toMemberDeviceResponse(mps);
        });
    }

    @Override
    public long countMy(Long memberId) {
        return memberPraiseSongRepository.countByMemberId(memberId);
    }

    // ── private: Entity → DTO 변환 ──

    private PraiseResponse toResponse(PraiseSong song) {
        return new PraiseResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getSourceType().name(),
                song.getLicenseNote(),
                song.getStatus().name(),
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }

    private MemberPraiseSongResponse toMemberResponse(MemberPraiseSong mps,
                                                       String songTitle,
                                                       String songArtist,
                                                       String sourceType) {
        return new MemberPraiseSongResponse(
                mps.getId(),
                mps.getPraiseSongId(),
                mps.getDisplayTitle(),
                songTitle,
                songArtist,
                sourceType,
                mps.getDeviceSongKey(),
                mps.getCreatedAt()
        );
    }

    private MemberPraiseSongResponse toMemberDeviceResponse(MemberPraiseSong mps) {
        return new MemberPraiseSongResponse(
                mps.getId(),
                null,
                mps.getDisplayTitle(),
                null,
                null,
                PraiseSourceType.DEVICE.name(),
                mps.getDeviceSongKey(),
                mps.getCreatedAt()
        );
    }
}
