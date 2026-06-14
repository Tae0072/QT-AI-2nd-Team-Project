package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.ListBookmarksUseCase;
import com.qtai.domain.sharing.api.ToggleBookmarkUseCase;
import com.qtai.domain.sharing.api.dto.BookmarkResponse;
import com.qtai.domain.sharing.api.dto.SharingPostListItem;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.VerseSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 나눔 게시글 저장(북마크) 서비스. 저장·해제·내 저장 목록을 담당한다.
 *
 * <p>좋아요({@link SharingPostService}의 like/unlike)와 같은 정책을 따른다:
 * 저장 추가는 PUBLISHED 글만, 중복은 멱등(중복 INSERT 차단), 해제도 멱등.
 * 시각은 주입 Clock(Asia/Seoul) 기준으로 기록한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharingBookmarkService implements ToggleBookmarkUseCase, ListBookmarksUseCase {

    private static final int PREVIEW_LENGTH = 100;

    private final SharingBookmarkRepository sharingBookmarkRepository;
    private final SharingPostRepository sharingPostRepository;
    // 저장 목록의 likedByMe 계산용(피드와 동일 카드 렌더링).
    private final PostLikeRepository postLikeRepository;
    // 공통 시계(Asia/Seoul) — 저장 시각 기록(목록 정렬 기준).
    private final Clock clock;

    /**
     * 저장(북마크) 추가. PUBLISHED 게시글만 저장할 수 있고, 이미 저장돼 있으면
     * 중복 INSERT 없이 bookmarked=true로 끝낸다(멱등). (sharingPostId, memberId) UNIQUE가 최종 backstop.
     */
    @Override
    @Transactional
    public BookmarkResponse bookmark(Long memberId, Long postId) {
        sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));
        if (!sharingBookmarkRepository.existsBySharingPostIdAndMemberId(postId, memberId)) {
            sharingBookmarkRepository.save(SharingBookmark.of(postId, memberId, LocalDateTime.now(clock)));
        }
        return new BookmarkResponse(true);
    }

    /**
     * 저장 해제. 멱등 — 저장한 적 없어도 0건 삭제로 조용히 끝낸다.
     * 이미 숨김·삭제된 글이라도 저장 정리를 위해 해제는 허용한다(상태 검증 없음).
     */
    @Override
    @Transactional
    public void unbookmark(Long memberId, Long postId) {
        sharingBookmarkRepository.deleteBySharingPostIdAndMemberId(postId, memberId);
    }

    /**
     * 내 저장 목록. 내가 저장한 글 중 현재 PUBLISHED인 글만 최근 저장순으로 반환한다.
     * 정렬은 리포지토리 쿼리(b.id DESC)가 고정하므로 Pageable의 sort는 무시하고 page/size만 쓴다.
     */
    @Override
    public SharingPostListResponse listBookmarks(Long memberId, Pageable pageable) {
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<SharingPost> page = sharingBookmarkRepository.findBookmarkedPosts(
                memberId, SharingPostStatus.PUBLISHED, paging);

        Set<Long> likedPostIds = findLikedPostIds(memberId, page.getContent());

        List<SharingPostListItem> content = page.getContent().stream()
                .map(post -> toItem(post, likedPostIds))
                .toList();

        return new SharingPostListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                "bookmarkedAt,desc");
    }

    /** SharingPost → 피드 목록 항목. 저장 목록이므로 bookmarkedByMe는 항상 true. */
    private SharingPostListItem toItem(SharingPost post, Set<Long> likedPostIds) {
        return new SharingPostListItem(
                post.getId(),
                post.getNicknameSnapshot(),
                post.getSnapshotTitle(),
                post.getSnapshotCategory(),
                post.getStatus().name(),
                new VerseSnapshot(post.getSnapshotVerseLabel()),
                toPreview(post.getSnapshotBody()),
                post.isCommentsEnabled(),
                post.getSourceNoteUnsharedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedPostIds.contains(post.getId()),
                true,
                post.getCreatedAt());
    }

    /** likedByMe 배치 조회: 이 페이지 글 id들 중 내가 누른 것만 1회에 모은다(N+1 방지). */
    private Set<Long> findLikedPostIds(Long memberId, List<SharingPost> posts) {
        if (posts.isEmpty()) {
            return Set.of();
        }
        List<Long> postIds = posts.stream().map(SharingPost::getId).toList();
        return new HashSet<>(postLikeRepository.findLikedPostIds(memberId, postIds));
    }

    private String toPreview(String body) {
        if (body == null || body.length() <= PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, PREVIEW_LENGTH) + "…";
    }
}
