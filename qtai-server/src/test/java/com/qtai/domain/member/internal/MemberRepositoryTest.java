package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

/**
 * MemberRepository 통합 테스트.
 *
 * <p>H2 create-drop 으로 derived query·UK 검증.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    // ── findByKakaoId ──

    @Test
    @DisplayName("findByKakaoId — 존재하는 kakaoId 조회 성공")
    void findByKakaoId_found() {
        Member member = Member.builder()
                .kakaoId(12345L)
                .nickname("테스터")
                .email("test@test.com")
                .build();
        em.persistAndFlush(member);
        em.clear();

        Optional<Member> found = memberRepository.findByKakaoId(12345L);

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("findByKakaoId — 존재하지 않는 kakaoId 는 empty")
    void findByKakaoId_not_found() {
        Optional<Member> found = memberRepository.findByKakaoId(99999L);

        assertThat(found).isEmpty();
    }

    // ── existsByNickname ──

    @Test
    @DisplayName("existsByNickname — 존재하면 true")
    void existsByNickname_true() {
        Member member = Member.builder()
                .kakaoId(100L)
                .nickname("고유닉네임")
                .build();
        em.persistAndFlush(member);

        assertThat(memberRepository.existsByNickname("고유닉네임")).isTrue();
    }

    @Test
    @DisplayName("existsByNickname — 존재하지 않으면 false")
    void existsByNickname_false() {
        assertThat(memberRepository.existsByNickname("없는닉네임")).isFalse();
    }

    // ── UK: kakao_id ──

    @Test
    @DisplayName("UK kakao_id — 동일 kakaoId 중복 삽입 시 예외")
    void unique_constraint_kakaoId() {
        em.persistAndFlush(Member.builder()
                .kakaoId(200L).nickname("첫번째").build());

        Member dup = Member.builder()
                .kakaoId(200L).nickname("두번째").build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> em.persistAndFlush(dup));
    }

    // ── UK: nickname ──

    @Test
    @DisplayName("UK nickname — 동일 닉네임 중복 삽입 시 예외")
    void unique_constraint_nickname() {
        em.persistAndFlush(Member.builder()
                .kakaoId(300L).nickname("중복닉네임").build());

        Member dup = Member.builder()
                .kakaoId(301L).nickname("중복닉네임").build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> em.persistAndFlush(dup));
    }
}
