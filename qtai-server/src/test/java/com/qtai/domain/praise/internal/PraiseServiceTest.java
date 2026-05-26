package com.qtai.domain.praise.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;

/**
 * PraiseService 단위 테스트.
 */
class PraiseServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-26T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private PraiseSongRepository praiseSongRepository;
    private MemberPraiseSongRepository memberPraiseSongRepository;
    private PraiseService praiseService;

    @BeforeEach
    void setUp() {
        praiseSongRepository = Mockito.mock(PraiseSongRepository.class);
        memberPraiseSongRepository = Mockito.mock(MemberPraiseSongRepository.class);
        praiseService = new PraiseService(praiseSongRepository, memberPraiseSongRepository, FIXED_CLOCK);
    }

    // ── create (ADMIN) ──

    @Test
    void create_큐레이션_곡_등록_성공() {
        PraiseCreateRequest request = new PraiseCreateRequest("Amazing Grace", "John Newton", null);
        when(praiseSongRepository.save(any(PraiseSong.class)))
                .thenAnswer(inv -> {
                    PraiseSong song = inv.getArgument(0);
                    setId(song, 1L);
                    return song;
                });

        PraiseResponse response = praiseService.create(100L, request);

        assertThat(response.title()).isEqualTo("Amazing Grace");
        assertThat(response.sourceType()).isEqualTo("CURATED");
    }

    // ── listActive ──

    @Test
    void listActive_활성_곡_목록_조회() {
        PraiseSong s1 = createPraiseSong(1L, "Song A", "Artist A");
        PraiseSong s2 = createPraiseSong(2L, "Song B", "Artist B");
        Pageable pageable = PageRequest.of(0, 10);
        when(praiseSongRepository.findByStatus(eq("ACTIVE"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(s1, s2)));

        Page<PraiseResponse> result = praiseService.listActive(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("Song A");
        assertThat(result.getContent().get(1).sourceType()).isEqualTo("CURATED");
    }

    // ── save (내 찬양 저장) ──

    @Test
    void save_큐레이션_곡_저장_성공() {
        PraiseSong song = createPraiseSong(1L, "Test Song", "Artist");
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 1L)).thenReturn(false);
        when(praiseSongRepository.findById(1L)).thenReturn(Optional.of(song));
        when(memberPraiseSongRepository.save(any(MemberPraiseSong.class)))
                .thenAnswer(inv -> {
                    MemberPraiseSong mps = inv.getArgument(0);
                    setId(mps, 10L);
                    return mps;
                });

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(1L, null, "My Song");
        MemberPraiseSongResponse response = praiseService.save(1L, request);

        assertThat(response.displayTitle()).isEqualTo("My Song");
        assertThat(response.title()).isEqualTo("Test Song");
    }

    @Test
    void save_이미_저장된_곡_중복_에러() {
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 1L)).thenReturn(true);

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(1L, null, "Dup");

        assertThatThrownBy(() -> praiseService.save(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
    }

    @Test
    void save_존재하지_않는_큐레이션_곡() {
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 999L)).thenReturn(false);
        when(praiseSongRepository.findById(999L)).thenReturn(Optional.empty());

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(999L, null, "Not Exist");

        assertThatThrownBy(() -> praiseService.save(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRAISE_SONG_NOT_FOUND);
    }

    @Test
    void save_큐레이션_곡_TOCTOU_UK위반_ALREADY_SAVED() {
        PraiseSong song = createPraiseSong(1L, "Song", "Artist");
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 1L)).thenReturn(false);
        when(praiseSongRepository.findById(1L)).thenReturn(Optional.of(song));
        when(memberPraiseSongRepository.save(any(MemberPraiseSong.class)))
                .thenThrow(new DataIntegrityViolationException("UK violation"));

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(1L, null, "Race");

        assertThatThrownBy(() -> praiseService.save(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
    }

    @Test
    void save_디바이스_곡_TOCTOU_UK위반_ALREADY_SAVED() {
        when(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(1L, "device://race"))
                .thenReturn(false);
        when(memberPraiseSongRepository.save(any(MemberPraiseSong.class)))
                .thenThrow(new DataIntegrityViolationException("UK violation"));

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(
                null, "device://race", "Race Device");

        assertThatThrownBy(() -> praiseService.save(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRAISE_SONG_ALREADY_SAVED);
    }

    @Test
    void save_디바이스_곡_등록_성공() {
        when(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(1L, "device://song123"))
                .thenReturn(false);
        when(memberPraiseSongRepository.save(any(MemberPraiseSong.class)))
                .thenAnswer(inv -> {
                    MemberPraiseSong mps = inv.getArgument(0);
                    setId(mps, 20L);
                    return mps;
                });

        MemberPraiseSongCreateRequest request = new MemberPraiseSongCreateRequest(
                null, "device://song123", "Device Song");
        MemberPraiseSongResponse response = praiseService.save(1L, request);

        assertThat(response.sourceType()).isEqualTo("DEVICE");
        assertThat(response.deviceSongKey()).isEqualTo("device://song123");
    }

    // ── remove ──

    @Test
    void remove_성공() {
        MemberPraiseSong mps = MemberPraiseSong.builder()
                .memberId(1L)
                .displayTitle("To Remove")
                .build();
        setId(mps, 10L);
        when(memberPraiseSongRepository.findByIdAndMemberId(10L, 1L))
                .thenReturn(Optional.of(mps));

        praiseService.remove(1L, 10L);

        verify(memberPraiseSongRepository).delete(mps);
    }

    @Test
    void remove_존재하지_않는_저장곡() {
        when(memberPraiseSongRepository.findByIdAndMemberId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> praiseService.remove(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRAISE_SONG_SAVE_NOT_FOUND);
    }

    // ── listMy ──

    @Test
    void listMy_큐레이션_곡_포함_목록_조회() {
        PraiseSong song = createPraiseSong(1L, "Curated Song", "Artist");
        MemberPraiseSong mps = MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(1L)
                .displayTitle("My Curated")
                .createdAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
        setId(mps, 10L);
        Pageable pageable = PageRequest.of(0, 10);
        when(memberPraiseSongRepository.findByMemberIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mps)));
        when(praiseSongRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(song));

        Page<MemberPraiseSongResponse> result = praiseService.listMy(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        MemberPraiseSongResponse resp = result.getContent().get(0);
        assertThat(resp.title()).isEqualTo("Curated Song");
        assertThat(resp.sourceType()).isEqualTo("CURATED");
        assertThat(resp.displayTitle()).isEqualTo("My Curated");
    }

    @Test
    void listMy_디바이스_곡_전용_목록_조회() {
        MemberPraiseSong mps = MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://local123")
                .displayTitle("Device Song")
                .createdAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
        setId(mps, 20L);
        Pageable pageable = PageRequest.of(0, 10);
        when(memberPraiseSongRepository.findByMemberIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mps)));

        Page<MemberPraiseSongResponse> result = praiseService.listMy(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        MemberPraiseSongResponse resp = result.getContent().get(0);
        assertThat(resp.sourceType()).isEqualTo("DEVICE");
        assertThat(resp.deviceSongKey()).isEqualTo("device://local123");
        assertThat(resp.praiseSongId()).isNull();
    }

    @Test
    void listMy_큐레이션_디바이스_혼합_목록() {
        PraiseSong song = createPraiseSong(1L, "Curated", "Artist");
        MemberPraiseSong curated = MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(1L)
                .displayTitle("C")
                .createdAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
        setId(curated, 10L);
        MemberPraiseSong device = MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://x")
                .displayTitle("D")
                .createdAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
        setId(device, 20L);

        Pageable pageable = PageRequest.of(0, 10);
        when(memberPraiseSongRepository.findByMemberIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(curated, device)));
        when(praiseSongRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(song));

        Page<MemberPraiseSongResponse> result = praiseService.listMy(1L, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).sourceType()).isEqualTo("CURATED");
        assertThat(result.getContent().get(1).sourceType()).isEqualTo("DEVICE");
    }

    // ── countMy ──

    @Test
    void countMy_저장_곡_수_반환() {
        when(memberPraiseSongRepository.countByMemberId(1L)).thenReturn(7L);

        long count = praiseService.countMy(1L);

        assertThat(count).isEqualTo(7);
    }

    // ── helpers ──

    private PraiseSong createPraiseSong(Long id, String title, String artist) {
        PraiseSong song = PraiseSong.builder()
                .title(title)
                .artist(artist)
                .sourceType("CURATED")
                .status("ACTIVE")
                .build();
        setId(song, id);
        return song;
    }

    private void setId(Object entity, Long id) {
        try {
            // BaseEntity 또는 직접 id 필드
            Class<?> clazz = entity.getClass();
            java.lang.reflect.Field field = null;
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField("id");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
