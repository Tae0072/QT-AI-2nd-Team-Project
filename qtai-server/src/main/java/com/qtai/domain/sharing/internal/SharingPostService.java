package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.sharing.api.DeleteSharingPostUseCase;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListMySharingPostsUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.PublishNoteUseCase;
import com.qtai.domain.sharing.api.SharingPostVisibilityUseCase;
import com.qtai.domain.sharing.api.ToggleLikeUseCase;
import com.qtai.domain.sharing.api.dto.LikeResponse;
import com.qtai.domain.sharing.api.dto.MySharingPostListItem;
import com.qtai.domain.sharing.api.dto.MySharingPostListResponse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharingPostService
        implements ListSharingPostsUseCase, ListMySharingPostsUseCase, GetSharingPostUseCase, PublishNoteUseCase,
        ToggleLikeUseCase, DeleteSharingPostUseCase, SharingPostVisibilityUseCase {

    private static final int PREVIEW_LENGTH = 100;

    // API 노출 정렬 필드 → 엔티티 필드 매핑(화이트리스트). 목록에 없는 필드는 무시한다.
    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "publishedAt", "createdAt",
            "likeCount", "likeCount",
            "commentCount", "commentCount");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    // 내 나눔 목록(M-05) status 생략 시 기본: 공개+숨김. 삭제본(DELETED)은 항상 제외한다.
    private static final List<SharingPostStatus> DEFAULT_MY_STATUSES =
            List.of(SharingPostStatus.PUBLISHED, SharingPostStatus.HIDDEN);

    private final SharingPostRepository sharingPostRepository;
    private final PostLikeRepository postLikeRepository;
    // 다른 도메인은 api 포트로만 호출(CLAUDE.md §4). 노트 본문 조회·작성자 닉네임 조회용.
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

    /**
     * 내 나눔 목록(04 §4.4.5, 화면 M-05). 작성자 본인 글만 조회한다.
     * status가 null이면 PUBLISHED+HIDDEN을 함께, 명시하면 해당 상태만 거른다. DELETED·이상값은 400.
     */
    @Override
    public MySharingPostListResponse listMine(Long memberId, String status, Pageable pageable) {
        List<SharingPostStatus> statuses = resolveStatuses(status);

        Page<SharingPost> page = sharingPostRepository.findByMemberIdAndStatusIn(
                memberId, statuses, translateSort(pageable));

        List<MySharingPostListItem> content = page.getContent().stream()
                .map(this::toMyItem)
                .toList();

        return new MySharingPostListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                describeSort(pageable));
    }

    /**
     * status 쿼리 파라미터를 조회 대상 상태 목록으로 변환한다.
     * 생략(null/blank)이면 공개+숨김. PUBLISHED/HIDDEN만 단일 필터로 허용한다.
     * DELETED나 정의되지 않은 값은 삭제본 노출·오입력 방지를 위해 400(INVALID_INPUT)으로 막는다.
     */
    private List<SharingPostStatus> resolveStatuses(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return DEFAULT_MY_STATUSES;
        }
        if ("PUBLISHED".equalsIgnoreCase(normalized)) {
            return List.of(SharingPostStatus.PUBLISHED);
        }
        if ("HIDDEN".equalsIgnoreCase(normalized)) {
            return List.of(SharingPostStatus.HIDDEN);
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    /** SharingPost → 내 나눔 목록 항목. 공개 피드 항목보다 적은 9필드만 담는다(04 §4.4.5). */
    private MySharingPostListItem toMyItem(SharingPost post) {
        return new MySharingPostListItem(
                post.getId(),
                post.getSnapshotTitle(),
                post.getSnapshotCategory(),
                post.getStatus().name(),
                post.isCommentsEnabled(),
                post.getSourceNoteUnsharedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt());
    }

    @Override
    public SharingPostResponse getDetail(Long memberId, Long postId) {
        SharingPost post = sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));

        boolean likedByMe = !postLikeRepository.findLikedPostIds(memberId, List.of(postId)).isEmpty();
        boolean ownedByMe = post.getMemberId().equals(memberId);

        return toDetail(post, likedByMe, ownedByMe);
    }

    /**
     * 노트를 나눔으로 공개한다(F-10). 공개 시점 값을 SharingPost에 스냅샷으로 박제하므로
     * 이후 원본 노트·닉네임이 바뀌어도 게시글은 변하지 않는다.
     *
     * <p>한 트랜잭션 안에서 검증→스냅샷 생성→저장이 모두 일어나므로, 중간에 실패하면 전체 롤백된다(원자성).
     */
    @Override
    @Transactional
    public SharingPostResponse publish(Long memberId, Long noteId, PublishNoteRequest request) {
        // 1. 닉네임 공개 동의(true)가 아니면 공유하지 않는다. (@NotNull로 null은 이미 400에서 걸림)
        if (!Boolean.TRUE.equals(request.confirmNicknamePublic())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 2. 노트 조회 — 본인 소유·존재 검증은 note 도메인이 수행(없으면 NOTE_NOT_FOUND).
        NoteDetailResponse note = getNoteUseCase.get(memberId, noteId);
        // 3. 저장 확정본(SAVED)만 공유 가능. 임시저장·삭제본은 공유 불가(04 §4.3.8).
        if (note.status() != NoteStatus.SAVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 4. 같은 노트는 한 번만 공개(noteId UNIQUE). 사전 조회로 친절한 409, DB 제약이 최종 backstop.
        if (sharingPostRepository.existsByNoteId(noteId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SHARING_POST);
        }
        // 5. 작성자 닉네임 박제(member 도메인에서 조회).
        String nickname = getMemberUseCase.getMember(memberId).nickname();
        // 6. 댓글 허용은 요청이 생략(null)하면 기본 true.
        boolean commentsEnabled = request.commentsEnabled() == null || request.commentsEnabled();

        SharingPost post = SharingPost.publish(
                memberId,
                noteId,
                note.title(),
                composeBody(note),
                note.category().name(),
                note.qtDate(),
                note.rangeLabel(),
                nickname,
                commentsEnabled);
        SharingPost saved = sharingPostRepository.save(post);

        // 방금 내가 만든 글이므로 likedByMe=false, ownedByMe=true.
        return toDetail(saved, false, true);
    }

    /**
     * 좋아요(F-10). PUBLISHED 게시글에만, 중복은 409로 막고, likeCount는 실제 행 수로 재계산한다.
     */
    @Override
    @Transactional
    public LikeResponse like(Long memberId, Long postId) {
        SharingPost post = sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));
        // 이미 누른 좋아요면 409. (post_likes UNIQUE가 동시성 최종 backstop)
        if (postLikeRepository.existsBySharingPostIdAndMemberId(postId, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LIKE);
        }
        postLikeRepository.save(PostLike.of(postId, memberId));
        // 실제 행 수를 세어 likeCount에 반영(COUNT 재계산). post는 관리 엔티티라 dirty checking으로 UPDATE.
        long count = postLikeRepository.countBySharingPostId(postId);
        post.syncLikeCount(count);
        return new LikeResponse(count, true);
    }

    /**
     * 좋아요 취소(F-10). 멱등 — 누른 적 없어도 조용히 끝낸다. likeCount는 재계산한다.
     */
    @Override
    @Transactional
    public void unlike(Long memberId, Long postId) {
        SharingPost post = sharingPostRepository.findByIdAndStatus(postId, SharingPostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));
        // 행이 없어도 0건 삭제로 끝나 예외 없음(멱등).
        postLikeRepository.deleteBySharingPostIdAndMemberId(postId, memberId);
        long count = postLikeRepository.countBySharingPostId(postId);
        post.syncLikeCount(count);
    }

    /**
     * 나눔 게시글 삭제(F-10, 04 §4.4.6). 작성자 본인만, soft delete(status=DELETED), 멱등.
     */
    @Override
    @Transactional
    public void delete(Long memberId, Long postId) {
        SharingPost post = findOwnedPost(memberId, postId);
        if (post.getStatus() == SharingPostStatus.DELETED) {
            return; // 이미 삭제됨 — 멱등
        }
        post.delete(LocalDateTime.now());
    }

    /**
     * 나눔 게시글 공개 중단(F-10, 04 §4.4.6). 작성자 본인만, PUBLISHED→HIDDEN, 멱등.
     * 삭제된 글이면 엔티티 hide()가 INVALID_STATUS_TRANSITION을 던진다.
     */
    @Override
    @Transactional
    public void hide(Long memberId, Long postId) {
        SharingPost post = findOwnedPost(memberId, postId);
        if (post.getStatus() == SharingPostStatus.HIDDEN) {
            return; // 이미 숨김 — 멱등
        }
        post.hide(LocalDateTime.now());
    }

    /**
     * 나눔 게시글 되돌리기(F-10, 04 §4.4.6). 작성자 본인만, HIDDEN→PUBLISHED, 멱등.
     * 삭제된 글이면 엔티티 show()가 INVALID_STATUS_TRANSITION을 던진다.
     */
    @Override
    @Transactional
    public void show(Long memberId, Long postId) {
        SharingPost post = findOwnedPost(memberId, postId);
        if (post.getStatus() == SharingPostStatus.PUBLISHED) {
            return; // 이미 공개 — 멱등
        }
        post.show();
    }

    /**
     * 삭제·숨김·되돌리기 공통: 상태 무관 조회 + 작성자 본인 검증.
     * 없는 글은 404(SHARING_POST_NOT_FOUND), 남의 글은 403(FORBIDDEN).
     * PUBLISHED 한정 조회가 아니라 findById를 쓴다 — 숨김·삭제 상태도 본인 조작 대상이기 때문.
     */
    private SharingPost findOwnedPost(Long memberId, Long postId) {
        SharingPost post = sharingPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));
        if (!post.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    /**
     * 공유본 본문 스냅샷 구성. body가 있으면 그대로, 없으면(묵상 노트) 4섹션을 라벨 붙여 합친다.
     */
    private String composeBody(NoteDetailResponse note) {
        if (note.body() != null && !note.body().isBlank()) {
            return note.body();
        }
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "느낀 점", note.interpretSection());
        appendSection(sb, "기억할 구절", note.rememberSection());
        appendSection(sb, "적용할 점", note.applySection());
        appendSection(sb, "기도", note.praySection());
        return sb.toString().strip();
    }

    private void appendSection(StringBuilder sb, String label, String content) {
        if (content != null && !content.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("[").append(label).append("]\n").append(content.strip());
        }
    }

    /** SharingPost → 상세 응답 매핑. 조회(getDetail)와 공개(publish)가 공유한다. */
    private SharingPostResponse toDetail(SharingPost post, boolean likedByMe, boolean ownedByMe) {
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
