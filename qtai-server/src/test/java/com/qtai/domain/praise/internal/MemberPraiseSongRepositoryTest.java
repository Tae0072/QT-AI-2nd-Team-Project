package com.qtai.domain.praise.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

/**
 * MemberPraiseSongRepository 통합 테스트.
 *
 * <p>H2 create-drop 으로 복합 UK, derived query, 페이징 검증.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MemberPraiseSongRepositoryTest {

    @Autowired
    private MemberPraiseSongRepository memberPraiseSongRepository;

    @Autowired
    private PraiseSongRepository praiseSongRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 12, 0);

    @BeforeEach
    void setUp() {
        memberPraiseSongRepository.deleteAll();
        praiseSongRepository.deleteAll();
    }

    // ── UK: (member_id, praise_song_id) ──

    @Test
    @DisplayName("UK (member_id, praise_song_id) — 동일 회원+큐레이션곡 중복 삽입 시 예외")
    void unique_constraint_curated_duplicate() {
        PraiseSong song = praiseSongRepository.saveAndFlush(
                PraiseSong.builder().title("곡A").artist("Artist").build());

        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(song.getId())
                .displayTitle("첫 저장")
                .createdAt(NOW)
                .build());

        MemberPraiseSong dup = MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(song.getId())
                .displayTitle("중복 저장")
                .createdAt(NOW)
                .build();

        assertThrows(Exception.class, () -> em.persistAndFlush(dup));
    }

    @Test
    @DisplayName("UK (member_id, praise_song_id) — 다른 회원은 같은 곡 저장 가능")
    void unique_constraint_curated_different_member_ok() {
        PraiseSong song = praiseSongRepository.saveAndFlush(
                PraiseSong.builder().title("곡A").artist("Artist").build());

        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(song.getId())
                .displayTitle("회원1")
                .createdAt(NOW)
                .build());

        MemberPraiseSong other = MemberPraiseSong.builder()
                .memberId(2L)
                .praiseSongId(song.getId())
                .displayTitle("회원2")
                .createdAt(NOW)
                .build();

        em.persistAndFlush(other);
        assertThat(other.getId()).isNotNull();
    }

    // ── UK: (member_id, device_song_key) ──

    @Test
    @DisplayName("UK (member_id, device_song_key) — 동일 회원+디바이스키 중복 삽입 시 예외")
    void unique_constraint_device_duplicate() {
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://song123")
                .displayTitle("첫 등록")
                .createdAt(NOW)
                .build());

        MemberPraiseSong dup = MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://song123")
                .displayTitle("중복 등록")
                .createdAt(NOW)
                .build();

        assertThrows(Exception.class, () -> em.persistAndFlush(dup));
    }

    @Test
    @DisplayName("UK (member_id, device_song_key) — 다른 회원은 같은 키 저장 가능")
    void unique_constraint_device_different_member_ok() {
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://shared")
                .displayTitle("회원1")
                .createdAt(NOW)
                .build());

        MemberPraiseSong other = MemberPraiseSong.builder()
                .memberId(2L)
                .deviceSongKey("device://shared")
                .displayTitle("회원2")
                .createdAt(NOW)
                .build();
        em.persistAndFlush(other);
        assertThat(other.getId()).isNotNull();
    }

    // ── existsBy queries ──

    @Test
    @DisplayName("existsByMemberIdAndPraiseSongId — 정확한 조합만 true")
    void existsByMemberIdAndPraiseSongId() {
        PraiseSong song = praiseSongRepository.saveAndFlush(
                PraiseSong.builder().title("곡").artist("아티스트").build());

        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .praiseSongId(song.getId())
                .displayTitle("저장곡")
                .createdAt(NOW)
                .build());

        assertThat(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, song.getId())).isTrue();
        assertThat(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(2L, song.getId())).isFalse();
        assertThat(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 999L)).isFalse();
    }

    @Test
    @DisplayName("existsByMemberIdAndDeviceSongKey — 정확한 조합만 true")
    void existsByMemberIdAndDeviceSongKey() {
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://abc")
                .displayTitle("디바이스곡")
                .createdAt(NOW)
                .build());

        assertThat(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(1L, "device://abc")).isTrue();
        assertThat(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(1L, "device://other")).isFalse();
        assertThat(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(2L, "device://abc")).isFalse();
    }

    // ── findByIdAndMemberId ──

    @Test
    @DisplayName("findByIdAndMemberId — 본인 곡만 조회, 타인 곡 empty")
    void findByIdAndMemberId() {
        MemberPraiseSong mps = em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://mine")
                .displayTitle("내 곡")
                .createdAt(NOW)
                .build());

        assertThat(memberPraiseSongRepository.findByIdAndMemberId(mps.getId(), 1L)).isPresent();
        assertThat(memberPraiseSongRepository.findByIdAndMemberId(mps.getId(), 2L)).isEmpty();
    }

    // ── findByMemberIdOrderByCreatedAtDesc ──

    @Test
    @DisplayName("findByMemberIdOrderByCreatedAtDesc — 최신순 정렬 + 페이징")
    void findByMemberIdOrderByCreatedAtDesc() {
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://old")
                .displayTitle("오래된 곡")
                .createdAt(NOW.minusDays(1))
                .build());

        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L)
                .deviceSongKey("device://new")
                .displayTitle("최신 곡")
                .createdAt(NOW)
                .build());

        // 다른 회원
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(2L)
                .deviceSongKey("device://other")
                .displayTitle("타인 곡")
                .createdAt(NOW)
                .build());

        Page<MemberPraiseSong> page = memberPraiseSongRepository
                .findByMemberIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getDisplayTitle()).isEqualTo("최신 곡");
        assertThat(page.getContent().get(1).getDisplayTitle()).isEqualTo("오래된 곡");
    }

    // ── countByMemberId ──

    @Test
    @DisplayName("countByMemberId — 본인 곡 수만 카운트")
    void countByMemberId() {
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L).deviceSongKey("d://1").displayTitle("곡1").createdAt(NOW).build());
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(1L).deviceSongKey("d://2").displayTitle("곡2").createdAt(NOW).build());
        em.persistAndFlush(MemberPraiseSong.builder()
                .memberId(2L).deviceSongKey("d://3").displayTitle("타인곡").createdAt(NOW).build());

        assertThat(memberPraiseSongRepository.countByMemberId(1L)).isEqualTo(2);
        assertThat(memberPraiseSongRepository.countByMemberId(2L)).isEqualTo(1);
        assertThat(memberPraiseSongRepository.countByMemberId(999L)).isZero();
    }
}
