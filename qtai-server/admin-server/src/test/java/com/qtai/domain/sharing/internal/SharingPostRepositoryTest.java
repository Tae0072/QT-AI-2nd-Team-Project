package com.qtai.domain.sharing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 나눔 검색 JPQL {@link SharingPostRepository#searchForAdmin} 통합 테스트 (AD-15).
 *
 * <p>사용자 피드 검색과 달리 status=null이면 전체 상태(PUBLISHED/HIDDEN/DELETED)를 보고,
 * q는 제목·닉네임을 "포함" 검색하는지 H2에서 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SharingPostRepositoryTest {

    private static final LocalDate QT_DATE = LocalDate.parse("2026-06-16");
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-16T00:00:00");

    @Autowired
    private SharingPostRepository repository;

    @BeforeEach
    void setUp() {
        SharingPost published = SharingPost.publish(
                100L, 1001L, "아침 묵상", "본문1", "감사", QT_DATE, "시편 23:1", "철수", true);

        SharingPost hidden = SharingPost.publish(
                101L, 1002L, "저녁 기도", "본문2", "기도", QT_DATE, "시편 1:1", "영희", true);
        hidden.hide(NOW);

        SharingPost deleted = SharingPost.publish(
                102L, 1003L, "삭제될 글", "본문3", "감사", QT_DATE, null, "민수", true);
        deleted.delete(NOW);

        repository.saveAll(java.util.List.of(published, hidden, deleted));
        repository.flush();
    }

    @Test
    @DisplayName("status=null이면 전체 상태(공개/숨김/삭제)를 모두 본다")
    void searchForAdmin_nullStatus_returnsAllStatuses() {
        Page<SharingPost> page = repository.searchForAdmin(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(SharingPost::getStatus)
                .contains(SharingPostStatus.PUBLISHED, SharingPostStatus.HIDDEN, SharingPostStatus.DELETED);
    }

    @Test
    @DisplayName("status 지정 시 해당 상태만 반환한다")
    void searchForAdmin_statusFilter() {
        Page<SharingPost> hidden = repository.searchForAdmin(SharingPostStatus.HIDDEN, null, PageRequest.of(0, 10));

        assertThat(hidden.getContent()).isNotEmpty();
        assertThat(hidden.getContent()).allMatch(p -> p.getStatus() == SharingPostStatus.HIDDEN);
    }

    @Test
    @DisplayName("q는 제목을 포함 검색한다")
    void searchForAdmin_keywordMatchesTitle() {
        Page<SharingPost> page = repository.searchForAdmin(null, "아침", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(SharingPost::getSnapshotTitle).contains("아침 묵상");
    }

    @Test
    @DisplayName("q는 닉네임도 포함 검색한다")
    void searchForAdmin_keywordMatchesNickname() {
        Page<SharingPost> page = repository.searchForAdmin(null, "영희", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(SharingPost::getNicknameSnapshot).contains("영희");
    }
}
