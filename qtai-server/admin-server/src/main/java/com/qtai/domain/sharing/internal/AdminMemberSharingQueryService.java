package com.qtai.domain.sharing.internal;

import com.qtai.domain.sharing.api.AdminMemberSharingQueryUseCase;
import com.qtai.domain.sharing.api.dto.AdminMemberCommentItem;
import com.qtai.domain.sharing.api.dto.AdminMemberLikedPostItem;
import com.qtai.domain.sharing.api.dto.AdminMemberPostItem;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 상세용 나눔 활동 조회 서비스.
 *
 * <p>나눔 운영(AD-13) 전체 서비스와 분리된 가벼운 읽기 전용 구현. 회원이 공유한 글/작성한 댓글/
 * 좋아요한 글을 운영 식별에 필요한 메타데이터만 담아 최신순으로 돌려준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberSharingQueryService implements AdminMemberSharingQueryUseCase {

    private final SharingPostRepository sharingPostRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    @Override
    public Page<AdminMemberPostItem> listPostsByMember(Long memberId, Pageable pageable) {
        return sharingPostRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(p -> new AdminMemberPostItem(
                        p.getId(),
                        p.getStatus() == null ? null : p.getStatus().name(),
                        p.getSnapshotTitle(),
                        p.getSnapshotCategory(),
                        p.getCreatedAt()
                ));
    }

    @Override
    public Page<AdminMemberCommentItem> listCommentsByMember(Long memberId, Pageable pageable) {
        return commentRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(c -> new AdminMemberCommentItem(
                        c.getId(),
                        c.getSharingPostId(),
                        c.getBody(),
                        c.isDeleted(),
                        c.getCreatedAt()
                ));
    }

    @Override
    public Page<AdminMemberLikedPostItem> listLikedPostsByMember(Long memberId, Pageable pageable) {
        Page<PostLike> likes = postLikeRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        List<Long> postIds = likes.getContent().stream().map(PostLike::getSharingPostId).toList();
        Map<Long, SharingPost> postMap = postIds.isEmpty()
                ? Map.of()
                : sharingPostRepository.findAllById(postIds).stream()
                        .collect(Collectors.toMap(SharingPost::getId, Function.identity()));
        return likes.map(like -> {
            SharingPost p = postMap.get(like.getSharingPostId());
            return new AdminMemberLikedPostItem(
                    like.getSharingPostId(),
                    p == null ? null : p.getSnapshotTitle(),
                    p == null || p.getStatus() == null ? null : p.getStatus().name(),
                    like.getCreatedAt()
            );
        });
    }
}
