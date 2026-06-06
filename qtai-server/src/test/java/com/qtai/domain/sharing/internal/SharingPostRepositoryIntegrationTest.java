package com.qtai.domain.sharing.internal;

import com.qtai.config.JpaAuditingConfig;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SharingPostRepository 통합 테스트 (@DataJpaTest + H2).
 *
 * 단위 테스트(SharingPostServiceTest)는 Repository를 mock으로 두지만,
 * 이 테스트는 실제 H2에서 JPQL을 실행해 PUBLISHED 필터·category·q 검색이 SQL로 동작하는지 검증한다.
 * SharingPost는 빌더/팩토리가 없어 reflection으로 필드를 채워 영속화한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class SharingPostRepositoryIntegrationTest {

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired
    SharingPostRepository sharingPostRepository;
    @Autowired
    PostLikeRepository postLikeRepository;
    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("PUBLISHED 글만 조회되고 HIDDEN/DELETED는 제외된다")
    void search_publishedOnly() {
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "공개글1", "본문");
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "공개글2", "본문");
        persistPost(SharingPostStatus.HIDDEN, "MEDITATION", "숨김글", "본문");
        persistPost(SharingPostStatus.DELETED, "MEDITATION", "삭제글", "본문");
        em.flush();
        em.clear();

        Page<SharingPost> result = sharingPostRepository.search(
                SharingPostStatus.PUBLISHED, null, null, DEFAULT_PAGE);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getStatus() == SharingPostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("category로 필터링하면 해당 카테고리 글만 조회된다")
    void search_categoryFilter() {
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "묵상", "본문");
        persistPost(SharingPostStatus.PUBLISHED, "SERMON", "설교", "본문");
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "묵상2", "본문");
        em.flush();
        em.clear();

        Page<SharingPost> result = sharingPostRepository.search(
                SharingPostStatus.PUBLISHED, "MEDITATION", null, DEFAULT_PAGE);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getSnapshotCategory().equals("MEDITATION"));
    }

    @Test
    @DisplayName("q로 제목·본문 부분일치 검색 (OR LIKE)")
    void search_keyword() {
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "감사한 하루", "오늘 본문");
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "기도제목", "가족 건강 감사");
        persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "회개", "잘못한 일");
        em.flush();
        em.clear();

        Page<SharingPost> result = sharingPostRepository.search(
                SharingPostStatus.PUBLISHED, null, "감사", DEFAULT_PAGE);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByIdAndStatus는 PUBLISHED 글만 반환하고 HIDDEN/DELETED는 빈 결과다")
    void findByIdAndStatus_publishedOnly() {
        SharingPost published = persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "공개", "본문");
        SharingPost hidden = persistPost(SharingPostStatus.HIDDEN, "MEDITATION", "숨김", "본문");
        em.flush();
        em.clear();

        assertThat(sharingPostRepository.findByIdAndStatus(published.getId(), SharingPostStatus.PUBLISHED))
                .isPresent();
        assertThat(sharingPostRepository.findByIdAndStatus(hidden.getId(), SharingPostStatus.PUBLISHED))
                .isEmpty();
    }

    @Test
    @DisplayName("existsByNoteId는 이미 공개된 노트만 true, 없는 노트는 false")
    void existsByNoteId_detectsPublishedNote() {
        SharingPost post = persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "공개", "본문");
        em.flush();
        em.clear();

        assertThat(sharingPostRepository.existsByNoteId(post.getNoteId())).isTrue();
        assertThat(sharingPostRepository.existsByNoteId(999_999L)).isFalse();
    }

    @Test
    @DisplayName("note_id UNIQUE: 같은 노트를 두 번 공개하면 제약 위반으로 막힌다")
    void noteId_unique_blocksDuplicateShare() {
        SharingPost first = persistPost(SharingPostStatus.PUBLISHED, "MEDITATION", "공개1", "본문");
        em.flush();

        // 같은 noteId로 두 번째 공개 시도 → flush 시 UNIQUE 제약 위반
        SharingPost duplicate = new SharingPost();
        setField(duplicate, "memberId", 99L);
        setField(duplicate, "noteId", first.getNoteId());
        setField(duplicate, "status", SharingPostStatus.PUBLISHED);
        setField(duplicate, "snapshotTitle", "공개2");
        setField(duplicate, "snapshotBody", "본문");
        setField(duplicate, "snapshotCategory", "MEDITATION");
        setField(duplicate, "nicknameSnapshot", "하늘QT");

        assertThatThrownBy(() -> {
            em.persist(duplicate);
            em.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("post_likes UNIQUE: 같은 (글, 회원)이 두 번 좋아요면 제약 위반으로 막힌다")
    void postLike_unique_blocksDuplicate() {
        em.persist(PostLike.of(1L, 10L));
        em.flush();

        // 같은 (postId, memberId)로 두 번째 좋아요 → flush 시 UNIQUE(sharing_post_id, member_id) 위반
        assertThatThrownBy(() -> {
            em.persist(PostLike.of(1L, 10L));
            em.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("좋아요 existsBy·countBy·deleteBy: 추가·집계·멱등 취소가 SQL로 동작한다")
    void postLike_existsCountDelete() {
        em.persist(PostLike.of(1L, 10L)); // 글1에 회원10
        em.persist(PostLike.of(1L, 11L)); // 글1에 회원11
        em.flush();
        em.clear();

        assertThat(postLikeRepository.existsBySharingPostIdAndMemberId(1L, 10L)).isTrue();
        assertThat(postLikeRepository.existsBySharingPostIdAndMemberId(1L, 99L)).isFalse();
        assertThat(postLikeRepository.countBySharingPostId(1L)).isEqualTo(2L);

        postLikeRepository.deleteBySharingPostIdAndMemberId(1L, 10L);
        em.flush();
        assertThat(postLikeRepository.countBySharingPostId(1L)).isEqualTo(1L);

        // 멱등: 없는 좋아요를 취소해도 에러 없이 0건 삭제, 카운트 불변
        postLikeRepository.deleteBySharingPostIdAndMemberId(1L, 99L);
        em.flush();
        assertThat(postLikeRepository.countBySharingPostId(1L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("내 나눔(findByMemberIdAndStatusIn): 본인의 PUBLISHED+HIDDEN만 조회되고 남의 글·DELETED는 제외된다")
    void findByMemberIdAndStatusIn_ownPublishedAndHidden() {
        persistPostFor(7L, SharingPostStatus.PUBLISHED, "내 공개글");
        persistPostFor(7L, SharingPostStatus.HIDDEN, "내 숨김글");
        persistPostFor(7L, SharingPostStatus.DELETED, "내 삭제글");      // statuses에 없어 제외
        persistPostFor(8L, SharingPostStatus.PUBLISHED, "남의 공개글");  // 다른 member라 제외
        em.flush();
        em.clear();

        Page<SharingPost> result = sharingPostRepository.findByMemberIdAndStatusIn(
                7L, List.of(SharingPostStatus.PUBLISHED, SharingPostStatus.HIDDEN), DEFAULT_PAGE);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getMemberId().equals(7L));
        assertThat(result.getContent()).extracting(SharingPost::getStatus)
                .containsExactlyInAnyOrder(SharingPostStatus.PUBLISHED, SharingPostStatus.HIDDEN);
    }

    @Test
    @DisplayName("syncLikeCount: like_count를 실제 post_likes 행 수로 원자적 동기화한다 (P1-2)")
    void syncLikeCount_setsCountFromRows() {
        SharingPost post = persistPost(SharingPostStatus.PUBLISHED, "PRAYER", "글", "본문");
        // 카운터를 일부러 어긋난 값(99)으로 만들어 두고 동기화가 자가 치유하는지 확인
        setField(post, "likeCount", 99);
        em.persist(PostLike.of(post.getId(), 10L));
        em.persist(PostLike.of(post.getId(), 11L));
        em.flush();
        em.clear();

        sharingPostRepository.syncLikeCount(post.getId());

        SharingPost reloaded = sharingPostRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.getLikeCount()).isEqualTo(2); // 99 → 2로 자가 치유
    }

    @Test
    @DisplayName("syncCommentCount: comment_count를 삭제 제외 실제 행 수로 원자적 동기화한다 (P1-2)")
    void syncCommentCount_setsCountFromRows() {
        SharingPost post = persistPost(SharingPostStatus.PUBLISHED, "PRAYER", "글", "본문");
        setField(post, "commentCount", 99);
        em.persist(Comment.of(post.getId(), 10L, "댓글1"));
        em.persist(Comment.of(post.getId(), 11L, "댓글2"));
        Comment deleted = Comment.of(post.getId(), 12L, "삭제될 댓글");
        deleted.markDeleted();
        em.persist(deleted); // 삭제된 댓글은 카운트에서 제외돼야 함
        em.flush();
        em.clear();

        sharingPostRepository.syncCommentCount(post.getId());

        SharingPost reloaded = sharingPostRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.getCommentCount()).isEqualTo(2); // 삭제 1건 제외, 99 → 2
    }

    // ─────────────────────────────────────────────────────
    // 헬퍼 — noteId는 UNIQUE라 매 건 다른 값을 준다.
    // ─────────────────────────────────────────────────────

    private long noteIdSeq = 1L;

    /** memberId를 지정해 영속화한다. 내 나눔 조회의 member·status 필터 검증용. */
    private SharingPost persistPostFor(long memberId, SharingPostStatus status, String title) {
        SharingPost post = new SharingPost();
        setField(post, "memberId", memberId);
        setField(post, "noteId", noteIdSeq++);
        setField(post, "status", status);
        setField(post, "snapshotTitle", title);
        setField(post, "snapshotBody", "본문");
        setField(post, "snapshotCategory", "PRAYER");
        setField(post, "nicknameSnapshot", "하늘QT");
        em.persist(post);
        return post;
    }

    private SharingPost persistPost(SharingPostStatus status, String category, String title, String body) {
        SharingPost post = new SharingPost();
        setField(post, "memberId", 99L);
        setField(post, "noteId", noteIdSeq++);
        setField(post, "status", status);
        setField(post, "snapshotTitle", title);
        setField(post, "snapshotBody", body);
        setField(post, "snapshotCategory", category);
        setField(post, "nicknameSnapshot", "하늘QT");
        em.persist(post);
        return post;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
