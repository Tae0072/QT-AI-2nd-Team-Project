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

    // ─────────────────────────────────────────────────────
    // 헬퍼 — noteId는 UNIQUE라 매 건 다른 값을 준다.
    // ─────────────────────────────────────────────────────

    private long noteIdSeq = 1L;

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
