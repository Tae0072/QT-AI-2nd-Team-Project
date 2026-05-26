package com.qtai.domain.praise.internal;

import static org.assertj.core.api.Assertions.assertThat;

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
 * PraiseSongRepository 통합 테스트.
 *
 * <p>H2 create-drop 으로 findByStatus derived query 검증.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class PraiseSongRepositoryTest {

    @Autowired
    private PraiseSongRepository praiseSongRepository;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        praiseSongRepository.deleteAll();
    }

    @Test
    @DisplayName("findByStatus — ACTIVE 곡만 조회")
    void findByStatus_active_only() {
        PraiseSong active1 = PraiseSong.builder()
                .title("Active Song 1")
                .artist("Artist A")
                .sourceType(PraiseSourceType.CURATED)
                .status(PraiseSongStatus.ACTIVE)
                .build();
        PraiseSong active2 = PraiseSong.builder()
                .title("Active Song 2")
                .artist("Artist B")
                .sourceType(PraiseSourceType.CURATED)
                .status(PraiseSongStatus.ACTIVE)
                .build();
        PraiseSong hidden = PraiseSong.builder()
                .title("Hidden Song")
                .artist("Artist C")
                .sourceType(PraiseSourceType.CURATED)
                .status(PraiseSongStatus.HIDDEN)
                .build();
        em.persistAndFlush(active1);
        em.persistAndFlush(active2);
        em.persistAndFlush(hidden);
        em.clear();

        Page<PraiseSong> page = praiseSongRepository.findByStatus(
                PraiseSongStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(s -> s.getStatus() == PraiseSongStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByStatus — HIDDEN 곡만 조회")
    void findByStatus_hidden_only() {
        PraiseSong active = PraiseSong.builder()
                .title("Active Song")
                .sourceType(PraiseSourceType.CURATED)
                .status(PraiseSongStatus.ACTIVE)
                .build();
        PraiseSong hidden = PraiseSong.builder()
                .title("Hidden Song")
                .sourceType(PraiseSourceType.CURATED)
                .status(PraiseSongStatus.HIDDEN)
                .build();
        em.persistAndFlush(active);
        em.persistAndFlush(hidden);
        em.clear();

        Page<PraiseSong> page = praiseSongRepository.findByStatus(
                PraiseSongStatus.HIDDEN, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Hidden Song");
    }

    @Test
    @DisplayName("findByStatus — 해당 상태 곡이 없으면 빈 페이지")
    void findByStatus_empty() {
        Page<PraiseSong> page = praiseSongRepository.findByStatus(
                PraiseSongStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findByStatus — 페이징 동작 확인")
    void findByStatus_paging() {
        for (int i = 1; i <= 5; i++) {
            em.persistAndFlush(PraiseSong.builder()
                    .title("Song " + i)
                    .sourceType(PraiseSourceType.CURATED)
                    .status(PraiseSongStatus.ACTIVE)
                    .build());
        }
        em.clear();

        Page<PraiseSong> page = praiseSongRepository.findByStatus(
                PraiseSongStatus.ACTIVE, PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
