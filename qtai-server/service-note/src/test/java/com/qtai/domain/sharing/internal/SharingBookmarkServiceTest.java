package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.dto.BookmarkResponse;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 나눔 게시글 저장(북마크) 서비스 단위 테스트.
 *
 * <p>저장 대상 검증(없는 글 404), 중복 저장 멱등, 해제 멱등, 저장 목록 조회를 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class SharingBookmarkServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private SharingBookmarkRepository sharingBookmarkRepository;
    @Mock private SharingPostRepository sharingPostRepository;
    @Mock private PostLikeRepository postLikeRepository;

    private SharingBookmarkService service() {
        return new SharingBookmarkService(sharingBookmarkRepository, sharingPostRepository,
                postLikeRepository, CLOCK);
    }

    private static SharingPost publishedPost() {
        return SharingPost.publish(2L, 10L, "제목", "본문", "MEDITATION", null, null, "닉", true);
    }

    @Test
    void 저장_없는_글이면_SHARING_POST_NOT_FOUND() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().bookmark(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);
        verify(sharingBookmarkRepository, never()).save(any());
    }

    @Test
    void 저장_성공이면_INSERT하고_bookmarked_true() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost()));
        when(sharingBookmarkRepository.existsBySharingPostIdAndMemberId(5L, 1L)).thenReturn(false);

        BookmarkResponse response = service().bookmark(1L, 5L);

        assertThat(response.bookmarked()).isTrue();
        verify(sharingBookmarkRepository).save(any(SharingBookmark.class));
    }

    @Test
    void 저장_이미_저장됨이면_중복INSERT_없이_멱등() {
        when(sharingPostRepository.findByIdAndStatus(5L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost()));
        when(sharingBookmarkRepository.existsBySharingPostIdAndMemberId(5L, 1L)).thenReturn(true);

        BookmarkResponse response = service().bookmark(1L, 5L);

        assertThat(response.bookmarked()).isTrue();
        verify(sharingBookmarkRepository, never()).save(any());
    }

    @Test
    void 해제_멱등_삭제호출() {
        service().unbookmark(1L, 5L); // 예외 없이 통과

        verify(sharingBookmarkRepository).deleteBySharingPostIdAndMemberId(5L, 1L);
    }

    @Test
    void 저장목록_빈페이지면_빈_콘텐츠와_정렬표기() {
        when(sharingBookmarkRepository.findBookmarkedPosts(any(), any(), any()))
                .thenReturn(Page.empty());

        SharingPostListResponse response = service().listBookmarks(1L, PageRequest.of(0, 20));

        assertThat(response.content()).isEmpty();
        assertThat(response.sort()).isEqualTo("bookmarkedAt,desc");
    }
}
