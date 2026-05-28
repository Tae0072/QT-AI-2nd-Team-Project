package com.qtai.common;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.member.internal.Member;
import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.internal.JournalEvent;
import com.qtai.domain.note.internal.Note;
import com.qtai.domain.sharing.internal.PostLike;
import com.qtai.domain.sharing.internal.SharingPost;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class JpaEntityDdlTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private TestEntityManager testEm;

    @Test
    @DisplayName("JPA context loads core entity tables")
    void contextLoads_and_tables_created() {
        assertNotNull(em);
        assertNotNull(em.getMetamodel().entity(Member.class));
        assertNotNull(em.getMetamodel().entity(BibleBook.class));
        assertNotNull(em.getMetamodel().entity(Note.class));
        assertNotNull(em.getMetamodel().entity(JournalEvent.class));
        assertNotNull(em.getMetamodel().entity(SharingPost.class));
    }

    @Test
    @DisplayName("phase1 entities are registered in metamodel")
    void all_phase1_entities_in_metamodel() {
        assertNotNull(em.getMetamodel().entity(BibleBook.class));
        assertDoesNotThrow(() -> em.getMetamodel().entity(
                Class.forName("com.qtai.domain.qt.internal.QtPassage")));
        assertDoesNotThrow(() -> em.getMetamodel().entity(
                Class.forName("com.qtai.domain.note.internal.NoteVerse")));
        assertDoesNotThrow(() -> em.getMetamodel().entity(PostLike.class));
    }

    @Test
    @DisplayName("member can be persisted")
    void member_persist_and_flush() {
        Member member = Member.builder()
                .kakaoId(10001L)
                .nickname("normal-user")
                .build();

        Member saved = testEm.persistAndFlush(member);

        assertNotNull(saved.getId());
        assertEquals("normal-user", saved.getNickname());
    }

    @Test
    @DisplayName("member kakaoId is unique")
    void member_kakaoId_unique_constraint() {
        testEm.persistAndFlush(Member.builder().kakaoId(99999L).nickname("user-a").build());

        assertThrows(Exception.class, () ->
                testEm.persistAndFlush(Member.builder().kakaoId(99999L).nickname("user-b").build()));
    }

    @Test
    @DisplayName("member nickname is unique")
    void member_nickname_unique_constraint() {
        testEm.persistAndFlush(Member.builder().kakaoId(11111L).nickname("same-name").build());

        assertThrows(Exception.class, () ->
                testEm.persistAndFlush(Member.builder().kakaoId(22222L).nickname("same-name").build()));
    }

    @Test
    @DisplayName("active meditation note is unique per member and QT passage")
    void note_meditation_active_unique_constraint() {
        Note first = Note.builder()
                .memberId(1L)
                .qtPassageId(100L)
                .category(NoteCategory.MEDITATION)
                .title("meditation1")
                .body("body1")
                .build();
        testEm.persistAndFlush(first);
        assertEquals(Note.ACTIVE_KEY, first.getActiveUniqueKey());

        Note duplicate = Note.builder()
                .memberId(1L)
                .qtPassageId(100L)
                .category(NoteCategory.MEDITATION)
                .title("meditation2")
                .body("body2")
                .build();

        assertThrows(Exception.class, () -> testEm.persistAndFlush(duplicate));
    }

    @Test
    @DisplayName("journal event id is unique")
    void journalEvent_eventId_unique_constraint() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        JournalChangedEvent event = journalChangedEvent(eventId);
        testEm.persistAndFlush(JournalEvent.pending(event));

        JournalChangedEvent duplicateEvent = journalChangedEvent(eventId);

        assertThrows(Exception.class, () -> testEm.persistAndFlush(JournalEvent.pending(duplicateEvent)));
    }

    @Test
    @DisplayName("note lifecycle columns and active unique constraint are mapped")
    void note_lifecycle_columns_and_unique_constraint() throws Exception {
        Column activeUniqueKey = Note.class.getDeclaredField("activeUniqueKey").getAnnotation(Column.class);
        Column savedAt = Note.class.getDeclaredField("savedAt").getAnnotation(Column.class);
        Column deletedAt = findField(Note.class, "deletedAt").getAnnotation(Column.class);
        Table table = Note.class.getAnnotation(Table.class);

        assertEquals("active_unique_key", activeUniqueKey.name());
        assertTrue(activeUniqueKey.nullable());
        assertEquals("saved_at", savedAt.name());
        assertTrue(savedAt.nullable());
        assertEquals("deleted_at", deletedAt.name());
        assertTrue(deletedAt.nullable());
        assertTrue(Arrays.stream(table.uniqueConstraints())
                .anyMatch(uniqueConstraint -> "uk_notes_meditation_active".equals(uniqueConstraint.name())
                        && Arrays.equals(
                        new String[]{"member_id", "qt_passage_id", "active_unique_key"},
                        uniqueConstraint.columnNames()
                )));
    }

    @Test
    @DisplayName("post like is unique per post and member")
    void postLike_unique_constraint() throws Exception {
        PostLike like1 = createPostLike(500L, 1L);
        testEm.persistAndFlush(like1);
        assertNotNull(like1.getId());

        PostLike like2 = createPostLike(500L, 1L);
        assertThrows(Exception.class, () -> testEm.persistAndFlush(like2));
    }

    private PostLike createPostLike(Long sharingPostId, Long memberId) throws Exception {
        Constructor<PostLike> ctor = PostLike.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        PostLike like = ctor.newInstance();

        Field postIdField = PostLike.class.getDeclaredField("sharingPostId");
        postIdField.setAccessible(true);
        postIdField.set(like, sharingPostId);

        Field memberIdField = PostLike.class.getDeclaredField("memberId");
        memberIdField.setAccessible(true);
        memberIdField.set(like, memberId);

        return like;
    }

    private JournalChangedEvent journalChangedEvent(UUID eventId) {
        return new JournalChangedEvent(
                eventId,
                10L,
                99L,
                100L,
                JournalEventType.JOURNAL_UPDATED,
                NoteStatus.DRAFT,
                NoteStatus.SAVED,
                LocalDateTime.of(2026, 5, 17, 9, 0)
        );
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
