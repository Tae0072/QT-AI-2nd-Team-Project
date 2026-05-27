package com.qtai.domain.note.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

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

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;

/**
 * NoteRepository 통합 테스트 (@DataJpaTest + H2 in-memory).
 *
 * 단위 테스트(NoteServiceTest)는 Repository를 mock으로 두지만,
 * 이 테스트는 진짜 H2 DB에서 JPQL을 실행해
 * {@link NoteRepository#search} 의 동적 필터·페이지네이션·정렬이 실제 SQL로 잘 동작하는지 검증한다.
 *
 * <p>@DataJpaTest 는 JPA 슬라이스만 띄우고 일반 @Configuration 은 안 띄우므로,
 * createdAt/updatedAt 자동 채움(JpaAuditing)을 위해 @Import(JpaAuditingConfig.class) 명시.
 *
 * <p>@ActiveProfiles("test")로 test 프로파일을 활성화해
 * application-test.yml의 flyway.enabled=false를 적용한다. JPA 슬라이스 테스트는
 * Hibernate ddl-auto=create-drop으로 충분하며, Flyway 마이그레이션은 격리할 대상이다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class NoteRepositoryIntegrationTest {

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

    @Autowired
    NoteRepository noteRepository;
    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("본인 노트(memberId=10)만 조회되고 다른 사람(20) 노트는 안 보인다")
    void search_본인노트만() {
        // given
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "내 기도", "...");
        persistNote(10L, NoteCategory.GRATITUDE, NoteStatus.SAVED, "내 감사", "...");
        persistNote(20L, NoteCategory.PRAYER, NoteStatus.SAVED, "다른 사람 감사", "...");
        em.flush();
        em.clear();

        // when
        Page<Note> result = noteRepository.search(10L, null, null, null, DEFAULT_PAGE);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(n -> n.getMemberId() == 10L);
    }

    @Test
    @DisplayName("deletedAt 값이 있는 노트는 조회되지 않는다 (소프트 삭제)")
    void search_삭제된노트_제외() {
        // given
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "살아있는", "...");
        Note deleted = persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "삭제된", "...");
        setField(deleted, "deletedAt", LocalDateTime.now());
        em.flush();
        em.clear();

        // when
        Page<Note> result = noteRepository.search(10L, null, null, null, DEFAULT_PAGE);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("살아있는");
    }

    @Test
    @DisplayName("findActiveByIdAndMemberId는 삭제된 노트를 조회하지 않는다")
    void findActiveByIdAndMemberId_삭제된노트_제외() {
        Note active = persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "살아있는", "...");
        Note deleted = persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "삭제된", "...");
        setField(deleted, "deletedAt", LocalDateTime.now());
        em.flush();
        em.clear();

        assertThat(noteRepository.findActiveByIdAndMemberId(active.getId(), 10L)).isPresent();
        assertThat(noteRepository.findActiveByIdAndMemberId(deleted.getId(), 10L)).isEmpty();
    }

    @Test
    @DisplayName("category=PRAYER로 필터링하면 PRAYER 노트만 조회된다")
    void search_카테고리_필터() {
        // given
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "기도1", "...");
        persistNote(10L, NoteCategory.GRATITUDE, NoteStatus.SAVED, "감사1", "...");
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "기도2", "...");
        em.flush();
        em.clear();

        // when
        Page<Note> result = noteRepository.search(10L, NoteCategory.PRAYER, null, null, DEFAULT_PAGE);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(n -> n.getCategory() == NoteCategory.PRAYER);
    }

    @Test
    @DisplayName("status=DRAFT로 필터링하면 DRAFT 노트만 조회된다")
    void search_상태_필터() {
        // given
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "확정본", "...");
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.DRAFT, "임시본", "...");
        em.flush();
        em.clear();

        // when
        Page<Note> result = noteRepository.search(10L, null, NoteStatus.DRAFT, null, DEFAULT_PAGE);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(NoteStatus.DRAFT);
    }

    @Test
    @DisplayName("q 검색어로 제목·본문 부분일치 검색 (OR LIKE)")
    void search_키워드_검색() {
        // given
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "감사한 일", "오늘 가족");
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "기도제목", "가족 건강 감사");
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "회개", "잘못한 일");
        em.flush();
        em.clear();

        // when — "감사"가 제목 OR 본문에 포함된 노트
        Page<Note> result = noteRepository.search(10L, null, null, "감사", DEFAULT_PAGE);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("페이지네이션과 정렬(updatedAt desc)이 동작한다")
    void search_페이지네이션_정렬() throws InterruptedException {
        // given — 시간차로 3건 저장 (updatedAt 차이를 만들기 위해)
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "첫번째", "...");
        Thread.sleep(10);
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "두번째", "...");
        Thread.sleep(10);
        persistNote(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "세번째", "...");
        em.flush();
        em.clear();

        // when — page 0, size 2, updatedAt desc
        Page<Note> page0 = noteRepository.search(10L, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "updatedAt")));

        // then
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3L);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent().get(0).getTitle()).isEqualTo("세번째");
        assertThat(page0.getContent().get(1).getTitle()).isEqualTo("두번째");
    }

    // ─────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────

    /**
     * Note를 빌더로 만들고 status를 reflection으로 강제 세팅한 뒤 영속화한다.
     * (Note.builder()는 status를 DRAFT로 고정하기 때문)
     */
    private Note persistNote(Long memberId, NoteCategory category, NoteStatus status,
            String title, String body) {
        Note note = Note.builder()
                .memberId(memberId)
                .category(category)
                .title(title)
                .body(body)
                .build();
        setField(note, "status", status);
        em.persist(note);
        return note;
    }

    /**
     * 테스트 전용 reflection 헬퍼. Entity에 setter가 없는 필드를 강제 세팅.
     * BaseEntity의 상속 필드(deletedAt 등)도 자동으로 찾도록 슈퍼클래스를 거슬러 올라간다.
     */
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
