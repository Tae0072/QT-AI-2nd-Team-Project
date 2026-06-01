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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

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
