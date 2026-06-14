package com.qtai.domain.sharing.internal;

import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.notification.api.dto.NotificationSendRequest;
import com.qtai.domain.sharing.api.ListMentionsUseCase;
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
 * 나눔 '#닉네임' 멘션(태그) 서비스. 멘션 기록·알림과 "내가 태그된 글" 목록을 담당한다.
 *
 * <p>{@link #recordMentions}는 게시글 공개·댓글 작성 직후 같은 도메인(sharing.internal)에서 직접 호출한다.
 * 멘션은 사람(memberId)으로 저장하므로 닉네임 변경과 무관하게 목록·알림이 정확하다.
 * 멘션 해석·알림 실패는 게시글/댓글 작성 자체를 막지 않는다(비차단).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharingMentionService implements ListMentionsUseCase {

    private static final int PREVIEW_LENGTH = 100;

    private final SharingMentionRepository sharingMentionRepository;
    private final SharingPostRepository sharingPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final SharingBookmarkRepository sharingBookmarkRepository;
    // 닉네임 → 회원 해석(api 포트). 다른 도메인 직접 접근 금지(CLAUDE.md §4).
    private final GetMemberUseCase getMemberUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final Clock clock;

    /**
     * 본문의 '#닉네임'을 해석해 멘션을 기록하고, 멘션된 회원에게 알림을 보낸다.
     * 작성자 본인 멘션은 제외한다. 해석·알림 실패는 비차단(로그만).
     *
     * @param commentId 댓글 멘션이면 댓글 id, 게시글 본문 멘션이면 null
     */
    @Transactional
    public void recordMentions(Long sharingPostId, Long commentId, Long actorId, String text) {
        Set<String> nicknames = MentionTextParser.extractNicknames(text);
        if (nicknames.isEmpty()) {
            return;
        }
        final List<MemberPublicResponse> resolved;
        try {
            resolved = getMemberUseCase.resolveActiveByNicknames(nicknames);
        } catch (RuntimeException e) {
            log.warn("멘션 닉네임 해석 실패(비차단). postId={}, errorType={}",
                    sharingPostId, e.getClass().getSimpleName());
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> notified = new HashSet<>();
        for (MemberPublicResponse member : resolved) {
            Long mentionedId = member.id();
            // 본인 멘션 제외, 같은 텍스트 내 동일인 중복 제외.
            if (mentionedId == null || mentionedId.equals(actorId) || !notified.add(mentionedId)) {
                continue;
            }
            sharingMentionRepository.save(SharingMention.of(sharingPostId, commentId, mentionedId, now));
            notifyMentioned(mentionedId, sharingPostId, commentId);
        }
    }

    /** 멘션된 회원에게 알림. 사용자 콘텐츠(닉네임·본문)는 알림에 담지 않는다(CLAUDE.md §9). */
    private void notifyMentioned(Long mentionedId, Long postId, Long commentId) {
        try {
            String eventKey = "MENTION:" + postId + ":" + (commentId == null ? 0 : commentId) + ":" + mentionedId;
            sendNotificationUseCase.send(new NotificationSendRequest(
                    mentionedId, "MENTION", "멘션 알림", "나눔에서 회원님을 언급했어요.",
                    null, "SHARING_POST", postId, eventKey));
        } catch (RuntimeException e) {
            log.warn("멘션 알림 발송 실패(비차단). postId={}, errorType={}",
                    postId, e.getClass().getSimpleName());
        }
    }

    /**
     * 내가 태그된 글 목록. 내가 멘션된 글 중 PUBLISHED인 글만 중복 없이 최근 글 순으로 반환한다.
     * 정렬은 리포지토리 쿼리(sp.id DESC)가 고정하므로 Pageable의 sort는 무시하고 page/size만 쓴다.
     */
    @Override
    public SharingPostListResponse listMentions(Long memberId, Pageable pageable) {
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<SharingPost> page = sharingMentionRepository.findMentionedPosts(
                memberId, SharingPostStatus.PUBLISHED, paging);

        Set<Long> likedPostIds = batchIds(memberId, page.getContent(), true);
        Set<Long> bookmarkedPostIds = batchIds(memberId, page.getContent(), false);

        List<SharingPostListItem> content = page.getContent().stream()
                .map(post -> toItem(post, likedPostIds, bookmarkedPostIds))
                .toList();

        return new SharingPostListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                "publishedAt,desc");
    }

    /** liked/bookmarked 배치 조회(N+1 방지). liked=true면 좋아요, false면 저장 여부. */
    private Set<Long> batchIds(Long memberId, List<SharingPost> posts, boolean liked) {
        if (posts.isEmpty()) {
            return Set.of();
        }
        List<Long> postIds = posts.stream().map(SharingPost::getId).toList();
        return new HashSet<>(liked
                ? postLikeRepository.findLikedPostIds(memberId, postIds)
                : sharingBookmarkRepository.findBookmarkedPostIds(memberId, postIds));
    }

    private SharingPostListItem toItem(SharingPost post, Set<Long> likedPostIds, Set<Long> bookmarkedPostIds) {
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
                bookmarkedPostIds.contains(post.getId()),
                post.getCreatedAt());
    }

    private String toPreview(String body) {
        if (body == null || body.length() <= PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, PREVIEW_LENGTH) + "…";
    }
}
