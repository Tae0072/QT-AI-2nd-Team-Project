package com.qtai.domain.sharing.internal;

import com.qtai.domain.sharing.api.PurgeMemberSharingDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * sharing 도메인 — 회원 보존기간 만료 정리 구현.
 *
 * <p>자기 도메인 테이블(post_likes, comments, sharing_posts)만 삭제한다.
 * comments.parent_id 자기참조 FK 때문에 댓글 트리는 Java에서 transitive하게
 * 수집한 뒤 리프(자식 없는 댓글)부터 반복 삭제한다 — MySQL은 한 DELETE 문
 * 안에서도 행 단위로 FK를 검사하므로 부모·자식을 같은 문장에 섞을 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharingPurgeService implements PurgeMemberSharingDataUseCase {

    /** 댓글 트리 삭제 반복 한도 — 정상 데이터에선 트리 깊이만큼만 반복된다. */
    private static final int MAX_DELETE_PASSES = 50;

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        int deleted = 0;
        // 1) 좋아요 — 회원이 누른 것 + 회원 나눔 글에 달린 것(타인 포함)
        deleted += jdbc.update("DELETE FROM post_likes WHERE member_id = ? "
                + "OR sharing_post_id IN (SELECT id FROM sharing_posts WHERE member_id = ?)",
                memberId, memberId);
        // 2) 댓글 — 회원 댓글 + 회원 글의 댓글 트리(타인 대댓글 포함)
        deleted += deleteCommentTrees(memberId);
        // 3) 나눔 글 본체
        deleted += jdbc.update("DELETE FROM sharing_posts WHERE member_id = ?", memberId);
        return deleted;
    }

    /**
     * 삭제 대상 댓글(회원 본인 댓글 + 회원 글의 모든 댓글)과 그 대댓글 트리 전체를
     * 리프부터 반복 삭제한다.
     */
    private int deleteCommentTrees(Long memberId) {
        // ① 삭제 대상 수집: 본인 댓글 + 본인 글의 댓글 → 그 대댓글을 레벨 단위로 확장
        List<Long> doomed = jdbc.queryForList(
                "SELECT id FROM comments WHERE member_id = ? "
                        + "OR sharing_post_id IN (SELECT id FROM sharing_posts WHERE member_id = ?)",
                Long.class, memberId, memberId);
        if (doomed.isEmpty()) {
            return 0;
        }
        List<Long> frontier = doomed;
        while (!frontier.isEmpty()) {
            List<Long> children = jdbc.queryForList(
                    "SELECT id FROM comments WHERE parent_id IN (" + placeholders(frontier.size()) + ")",
                    Long.class, frontier.toArray());
            children.removeIf(doomed::contains);
            if (children.isEmpty()) {
                break;
            }
            doomed.addAll(children);
            frontier = children;
        }

        // ② 리프부터 반복 삭제 — 수집이 transitive하므로 정상 데이터에선 매 패스 리프가 존재
        int deleted = 0;
        List<Long> remaining = new ArrayList<>(doomed);
        int guard = 0;
        while (!remaining.isEmpty()) {
            if (++guard > MAX_DELETE_PASSES) {
                throw new IllegalStateException(
                        "댓글 트리 삭제 반복 한도 초과 — 데이터 확인 필요: memberId=" + memberId);
            }
            List<Long> stillParents = jdbc.queryForList(
                    "SELECT DISTINCT parent_id FROM comments WHERE parent_id IN ("
                            + placeholders(remaining.size()) + ")",
                    Long.class, remaining.toArray());
            List<Long> leaves = new ArrayList<>(remaining);
            leaves.removeAll(stillParents);
            if (leaves.isEmpty()) {
                // 남은 댓글 전부가 서로의 부모 = parent_id cycle (정상 FK 데이터에선 불가능)
                throw new IllegalStateException(
                        "댓글 parent_id cycle 감지 — 데이터 확인 필요: memberId=" + memberId
                                + ", remaining=" + remaining);
            }
            deleted += jdbc.update(
                    "DELETE FROM comments WHERE id IN (" + placeholders(leaves.size()) + ")",
                    leaves.toArray());
            remaining.retainAll(stillParents);
        }
        return deleted;
    }

    /** 빈 리스트에 호출하지 않는다 — 호출부에서 비어있지 않음을 보장한다. */
    private String placeholders(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("IN 절 placeholder 개수는 1 이상이어야 한다: " + n);
        }
        return String.join(",", Collections.nCopies(n, "?"));
    }
}
