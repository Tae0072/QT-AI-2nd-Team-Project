package com.qtai.domain.sharing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 원본 노트 삭제 통지(명세 §4.3.7 — 유지+안내 표시안) 테스트.
 */
class SharingSourceNoteServiceTest {

    private SharingPostRepository sharingPostRepository;
    private SharingSourceNoteService service;

    @BeforeEach
    void setUp() {
        sharingPostRepository = mock(SharingPostRepository.class);
        service = new SharingSourceNoteService(sharingPostRepository);
    }

    @Test
    @DisplayName("발행 글이 있으면 source_note_unshared_at을 기록하고 상태는 유지한다")
    void marksSourceNoteDeletedKeepingPublishedStatus() {
        SharingPost post = publishedPost();
        when(sharingPostRepository.findByNoteId(7L)).thenReturn(Optional.of(post));
        LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 5, 12, 0);

        service.markSourceNoteDeleted(7L, deletedAt);

        assertThat(post.getSourceNoteUnsharedAt()).isEqualTo(deletedAt);
        // 유지+안내 정책 — 게시글은 숨기지 않는다 (2026-06-05 Lead 결정)
        assertThat(post.getStatus()).isEqualTo(SharingPostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("이미 기록된 글은 최초 시각을 보존한다(멱등)")
    void keepsFirstRecordedTimestamp() {
        SharingPost post = publishedPost();
        LocalDateTime first = LocalDateTime.of(2026, 6, 5, 12, 0);
        post.markSourceNoteDeleted(first);
        when(sharingPostRepository.findByNoteId(7L)).thenReturn(Optional.of(post));

        service.markSourceNoteDeleted(7L, first.plusHours(1));

        assertThat(post.getSourceNoteUnsharedAt()).isEqualTo(first);
    }

    @Test
    @DisplayName("발행 글이 없는 노트는 아무것도 하지 않는다")
    void ignoresNotesWithoutPublishedPost() {
        when(sharingPostRepository.findByNoteId(7L)).thenReturn(Optional.empty());

        assertThatNoException()
                .isThrownBy(() -> service.markSourceNoteDeleted(7L, LocalDateTime.now()));
    }

    private static SharingPost publishedPost() {
        return SharingPost.publish(
                10L,
                7L,
                "제목",
                "본문",
                "MEDITATION",
                LocalDate.of(2026, 6, 1),
                "요한복음 3:16",
                "하늘QT",
                true
        );
    }
}
