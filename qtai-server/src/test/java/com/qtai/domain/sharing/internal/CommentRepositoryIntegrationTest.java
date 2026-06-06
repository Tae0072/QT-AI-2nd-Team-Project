package com.qtai.domain.sharing.internal;

import com.qtai.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommentRepository 통합 테스트 (@DataJpaTest + H2).
 *
 * 살아있는 댓글만(is_deleted=false) 시간순으로 조회/카운트하는 파생 쿼리가 실제 SQL로 동작하는지 검증한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class CommentRepositoryIntegrationTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Autowired
    CommentRepository commentRepository;
    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("findBy…IsDeletedFalseOrderByCreatedAtAsc: 삭제 댓글 제외 + 시간 오름차순 + 글 단위")
    void list_excludesDeleted_ordersByCreatedAt() {
        persist(1L, 10L, "글1-첫째", false);
        persist(1L, 11L, "글1-둘째", false);
        persist(1L, 12L, "글1-삭제됨", true);   // 삭제 → 제외돼야
        persist(2L, 10L, "글2-댓글", false);     // 다른 글 → 제외돼야
        em.flush();
        em.clear();

        Page<Comment> page = commentRepository
                .findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(1L, PAGE);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).noneMatch(Comment::isDeleted);
        // createdAt 오름차순 (오래된 → 최신)
        List<Comment> content = page.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt()).isBeforeOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("countBy…IsDeletedFalse: 살아있는 댓글만 센다")
    void count_excludesDeleted() {
        persist(1L, 10L, "a", false);
        persist(1L, 11L, "b", false);
        persist(1L, 12L, "c", true);   // 삭제 → 카운트 제외
        em.flush();
        em.clear();

        assertThat(commentRepository.countBySharingPostIdAndIsDeletedFalse(1L)).isEqualTo(2L);
    }

    private Comment persist(Long sharingPostId, Long memberId, String body, boolean deleted) {
        Comment comment = Comment.of(sharingPostId, memberId, body);
        if (deleted) {
            comment.markDeleted();
        }
        em.persist(comment);
        return comment;
    }
}
