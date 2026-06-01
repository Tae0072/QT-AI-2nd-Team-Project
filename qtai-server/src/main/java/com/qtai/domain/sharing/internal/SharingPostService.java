package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.PublishNoteUseCase;
import com.qtai.domain.sharing.api.dto.PublishNoteRequest;
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
public class SharingPostService implements ListSharingPostsUseCase, GetSharingPostUseCase, PublishNoteUseCase {

    private static final int PREVIEW_LENGTH = 100;

    // API 노출 정렬 필드 → 엔티티 필드 매핑(화이트리스트). 목록에 없는 필드는 무시한다.
    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "publishedAt", "createdAt",
            "likeCount", "likeCount",
            "commentCount", "commentCount");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final SharingPostRepository sharingPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final GetNoteUseCase getNoteUseCase;
    private final GetMemberUseCase getMemberUseCase;

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

    // ── 나눔 공유 (PublishNoteUseCase) ──

    @Override
    @Transactional
    public SharingPostResponse publish(Long memberId, Long noteId, PublishNoteRequest request) {
        // 1) 노트 조회 (본인 소유 + 존재 검증은 GetNoteUseCase에서 처리)
        NoteDetailResponse note = getNoteUseCase.get(memberId, noteId);

        // 2) 이미 공유된 노트인지 확인 (DDL note_id UNIQUE — 1:1 정책)
        if (sharingPostRepository.findByNoteId(noteId).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SHARING_POST);
        }

        // 3) 닉네임 조회
        MemberResponse member = getMemberUseCase.getMember(memberId);

        // 4) 스냅샷 생성 + 저장
        // verse 본문(절 텍스트)은 현재 미포함 — rangeLabel(구절 범위)만 스냅샷.
        // 다중 절 본문 스냅샷은 sharing_post_verses 테이블 추가 시 v2에서 확장 예정.
        SharingPost post = SharingPost.publish(
                memberId,
                noteId,
                member.nickname(),
                note.title(),
                note.body(),
                note.category().name(),
                note.qtDate(),
                note.rangeLabel(),
                request.isCommentsEnabled()
        );

        SharingPost saved;
        try {
            saved = sharingPostRepository.saveAndFlush(post);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_SHARING_POST);
        }

        return new SharingPostResponse(
                saved.getId(),
                saved.getNoteId(),
                saved.getMemberId(),
                saved.getNicknameSnapshot(),
                saved.getSnapshotTitle(),
                saved.getSnapshotBody(),
                saved.getSnapshotCategory(),
                new VerseSnapshotDetail(saved.getSnapshotVerseLabel(), List.of()),
                saved.isCommentsEnabled(),
                saved.getSourceNoteUnsharedAt(),
                saved.getStatus().name(),
                saved.getLikeCount(),
                saved.getCommentCount(),
                false,
                true,
                saved.getCreatedAt(),
                saved.getHiddenAt(),
                saved.getDeletedAt());
    }
}
