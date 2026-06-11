package com.qtai.domain.sharing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.common.config.TimeConfig;
import com.qtai.note.JpaAuditingConfig;
import com.qtai.note.NoteServiceApplication;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = NoteServiceApplication.class)
@Import({JpaAuditingConfig.class, TimeConfig.class})
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @DisplayName("신고 가능한 댓글이면 true를 반환한다")
    void existsReportableComment_returnsTrueForActiveCommentOnPublishedPost() {
        Comment comment = persistCommentOnPost(SharingPostStatus.PUBLISHED, false);

        assertThat(commentRepository.existsReportableComment(comment.getId())).isTrue();
    }

    @Test
    @DisplayName("삭제된 댓글이면 false를 반환한다")
    void existsReportableComment_returnsFalseForDeletedComment() {
        Comment comment = persistCommentOnPost(SharingPostStatus.PUBLISHED, true);

        assertThat(commentRepository.existsReportableComment(comment.getId())).isFalse();
    }

    @Test
    @DisplayName("부모 나눔 글이 숨김 상태이면 false를 반환한다")
    void existsReportableComment_returnsFalseForHiddenPostComment() {
        Comment comment = persistCommentOnPost(SharingPostStatus.HIDDEN, false);

        assertThat(commentRepository.existsReportableComment(comment.getId())).isFalse();
    }

    @Test
    @DisplayName("부모 나눔 글이 삭제 상태이면 false를 반환한다")
    void existsReportableComment_returnsFalseForDeletedPostComment() {
        Comment comment = persistCommentOnPost(SharingPostStatus.DELETED, false);

        assertThat(commentRepository.existsReportableComment(comment.getId())).isFalse();
    }

    private Comment persistCommentOnPost(SharingPostStatus postStatus, boolean deletedComment) {
        SharingPost post = SharingPost.publish(
                10L,
                nextNoteId(),
                "title",
                "body",
                "MEDITATION",
                null,
                null,
                "writer",
                true);
        if (postStatus == SharingPostStatus.HIDDEN) {
            post.hide(LocalDateTime.now());
        } else if (postStatus == SharingPostStatus.DELETED) {
            post.delete(LocalDateTime.now());
        }
        entityManager.persist(post);
        entityManager.flush();

        Comment comment = Comment.of(post.getId(), 20L, "comment");
        if (deletedComment) {
            comment.markDeleted();
        }
        entityManager.persist(comment);
        entityManager.flush();
        entityManager.clear();
        return comment;
    }

    private Long nextNoteId() {
        return System.nanoTime();
    }
}
