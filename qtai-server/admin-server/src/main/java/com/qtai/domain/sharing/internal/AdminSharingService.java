package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.AdminSharingPostUseCase;
import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 나눔 운영 서비스 (F-10, AD-15).
 *
 * <p>admin-server 고유 기능. 사용자용 {@code SharingPostService}와 달리 모든 상태를 조회하고,
 * 모더레이션은 게시글 엔티티의 {@code hide}/{@code show}(숨김/복원)만 사용한다(하드 삭제 없음).
 * 회원 통계·정리(purge)는 별도 서비스가 담당하므로 본 서비스에 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSharingService implements AdminSharingPostUseCase {

    private static final int BODY_PREVIEW_LENGTH = 120;

    private final SharingPostRepository sharingPostRepository;
    private final Clock clock;

    @Override
    public Page<AdminSharingPostResponse> listForAdmin(String status, String q, Pageable pageable) {
        SharingPostStatus statusFilter = parseStatusFilter(status);
        String keyword = (q == null || q.isBlank()) ? null : escapeLike(q.trim());
        // 목록은 본문 미리보기만(가벼움).
        return sharingPostRepository.searchForAdmin(statusFilter, keyword, pageable)
                .map(p -> toResponse(p, false));
    }

    @Override
    public AdminSharingPostResponse getForAdmin(Long postId) {
        return toResponse(load(postId), true);
    }

    @Override
    @Transactional
    public AdminSharingPostResponse hide(Long postId) {
        SharingPost post = load(postId);
        if (post.getStatus() == SharingPostStatus.HIDDEN) {
            return toResponse(post, true); // 멱등
        }
        if (post.getStatus() == SharingPostStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "삭제된 글은 숨길 수 없습니다.");
        }
        post.hide(LocalDateTime.now(clock));
        log.info("관리자 나눔 글 숨김. postId={}", postId);
        return toResponse(post, true);
    }

    @Override
    @Transactional
    public AdminSharingPostResponse restore(Long postId) {
        SharingPost post = load(postId);
        if (post.getStatus() == SharingPostStatus.PUBLISHED) {
            return toResponse(post, true); // 멱등
        }
        if (post.getStatus() == SharingPostStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "삭제된 글은 복원할 수 없습니다.");
        }
        post.show();
        log.info("관리자 나눔 글 복원(공개). postId={}", postId);
        return toResponse(post, true);
    }

    private SharingPost load(Long postId) {
        return sharingPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "나눔 글을 찾을 수 없습니다: " + postId));
    }

    private SharingPostStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SharingPostStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 PUBLISHED/HIDDEN/DELETED만 허용됩니다.");
        }
    }

    private String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private AdminSharingPostResponse toResponse(SharingPost post, boolean includeFullBody) {
        return new AdminSharingPostResponse(
                post.getId(),
                post.getMemberId(),
                post.getNicknameSnapshot(),
                post.getSnapshotTitle(),
                post.getSnapshotCategory(),
                post.getStatus().name(),
                preview(post.getSnapshotBody()),
                includeFullBody ? post.getSnapshotBody() : null,
                post.getSnapshotVerseLabel(),
                post.getSnapshotQtDate() == null ? null : post.getSnapshotQtDate().toString(),
                post.isCommentsEnabled(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getHiddenAt(),
                post.getSourceNoteUnsharedAt(),
                post.getCreatedAt()
        );
    }

    private String preview(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= BODY_PREVIEW_LENGTH ? body : body.substring(0, BODY_PREVIEW_LENGTH) + "…";
    }
}
