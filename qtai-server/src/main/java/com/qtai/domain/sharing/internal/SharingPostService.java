package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.dto.SharingPostListItem;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import com.qtai.domain.sharing.api.dto.VerseSnapshot;
import com.qtai.domain.sharing.api.dto.VerseSnapshotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharingPostService implements ListSharingPostsUseCase, GetSharingPostUseCase {

    private static final int PREVIEW_LENGTH = 100;

    // API 노출 정렬 필드 → 엔티티 필드 매핑(화이트리스트). 목록에 없는 필드는 무시한다.
    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "publishedAt", "createdAt",
            "likeCount", "likeCount",
            "commentCount", "commentCount");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final SharingPostRepository sharingPostRepository;
    private final PostLikeRepository postLikeRepository;

    @Override
    public SharingPostListResponse list(Long memberId, String category, String q, Pageable pageable) {
        String normalizedCategory = trimToNull(category);
        String escapedQuery = toEscapedQuery(q);

        Page<SharingPost> page = sharingPostRepository.search(
                SharingPostStatus.PUBLISHED, normalizedCategory, escapedQuery, translateSort(pageable));

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
                describeSort(pageable));
    }

    @Override
    public SharingPostResponse getDetail(Long memberId, Long postId) {
        SharingPost post = sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));

        boolean likedByMe = !postLikeRepository.findLikedPostIds(memberId, List.of(postId)).isEmpty();
        boolean ownedByMe = post.getMemberId().equals(memberId);

        return new SharingPostResponse(
                post.getId(),
                post.getNoteId(),
                post.getMemberId(),
                post.getNicknameSnapshot(),
                post.getSnapshotTitle(),
                post.getSnapshotBody(),
                post.getSnapshotCategory(),
                // verses[]는 다중 절 스냅샷 저장(sharing_post_verses)이 없어 빈 배열 (v2 작업)
                new VerseSnapshotDetail(post.getSnapshotVerseLabel(), List.of()),
                post.isCommentsEnabled(),
                post.getSourceNoteUnsharedAt(),
                post.getStatus().name(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByMe,
                ownedByMe,
                post.getCreatedAt(),
                post.getHiddenAt(),
                post.getDeletedAt());
    }

    /** likedByMe 배치 조회: 이 페이지 글 id들 중 내가 누른 것만 1회에 모아 Set으로. (N+1 방지) */
    private Set<Long> findLikedPostIds(Long memberId, List<SharingPost> posts) {
        if (posts.isEmpty()) {
            return Set.of();
        }
        List<Long> postIds = posts.stream().map(SharingPost::getId).toList();
        return new HashSet<>(postLikeRepository.findLikedPostIds(memberId, postIds));
    }

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
                post.getCreatedAt());
    }

    private String toPreview(String body) {
        if (body == null || body.length() <= PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, PREVIEW_LENGTH) + "…";
    }

    /** 사용자 입력 q의 LIKE 와일드카드(%, _, \)를 리터럴로 이스케이프. 빈 값이면 null(필터 생략). */
    private String toEscapedQuery(String q) {
        String trimmed = trimToNull(q);
        if (trimmed == null) {
            return null;
        }
        return trimmed
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** API 정렬 필드를 엔티티 필드로 변환. 허용 목록 밖이면 무시하고, 남는 게 없으면 기본 정렬. */
    private Pageable translateSort(Pageable pageable) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String mapped = SORT_FIELD_MAP.get(order.getProperty());
            if (mapped != null) {
                orders.add(new Sort.Order(order.getDirection(), mapped));
            }
        }
        Sort sort = orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /** 응답에 돌려줄 정렬 표기는 API 필드명 기준. 허용 필드가 없으면 기본값. */
    private String describeSort(Pageable pageable) {
        return pageable.getSort().stream()
                .filter(order -> SORT_FIELD_MAP.containsKey(order.getProperty()))
                .findFirst()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .orElse("publishedAt,desc");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
