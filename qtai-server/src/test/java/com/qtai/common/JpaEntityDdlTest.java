package com.qtai.common;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.member.internal.Member;
import com.qtai.domain.note.internal.Note;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.sharing.internal.PostLike;
import com.qtai.domain.sharing.internal.SharingPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 공통 기반 — H2 create-drop으로 Entity → DDL 자동 생성 검증.
 * Flyway off, Redis 불필요 (@DataJpaTest는 JPA 슬라이스만 로드).
 * JpaAuditingConfig를 Import하여 @CreatedDate/@LastModifiedDate 동작 보장.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class JpaEntityDdlTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private TestEntityManager testEm;

    @Test
    @DisplayName("JPA 컨텍스트 로드 — 모든 Entity 테이블이 H2에 생성된다")
    void contextLoads_and_tables_created() {
        assertNotNull(em);
        assertNotNull(em.getMetamodel().entity(Member.class));
        assertNotNull(em.getMetamodel().entity(BibleBook.class));
        assertNotNull(em.getMetamodel().entity(Note.class));
        assertNotNull(em.getMetamodel().entity(SharingPost.class));
    }

    @Test
    @DisplayName("모든 Phase 1 엔티티가 메타모델에 등록되어 있다")
    void all_phase1_entities_in_metamodel() {
        assertNotNull(em.getMetamodel().entity(BibleBook.class));
        assertDoesNotThrow(() -> em.getMetamodel().entity(
                Class.forName("com.qtai.domain.qt.internal.QtPassage")));
        assertDoesNotThrow(() -> em.getMetamodel().entity(
                Class.forName("com.qtai.domain.note.internal.NoteVerse")));
        assertDoesNotThrow(() -> em.getMetamodel().entity(
                Class.forName("com.qtai.domain.sharing.internal.PostLike")));
    }

    @Test
    @DisplayName("Member — 정상 persist/flush 성공")
    void member_persist_and_flush() {
        Member m = Member.builder()
                .kakaoId(10001L)
                .nickname("정상유저")
                .build();
        Member saved = testEm.persistAndFlush(m);
        assertNotNull(saved.getId());
        assertEquals("정상유저", saved.getNickname());
    }

    @Test
    @DisplayName("Member.kakao_id UNIQUE 제약 — 중복 kakaoId 삽입 시 예외 발생")
    void member_kakaoId_unique_constraint() {
        testEm.persistAndFlush(
                Member.builder().kakaoId(99999L).nickname("유저A").build());

        assertThrows(Exception.class, () ->
                testEm.persistAndFlush(
                        Member.builder().kakaoId(99999L).nickname("유저B").build()));
    }

    @Test
    @DisplayName("Member.nickname UNIQUE 제약 — 중복 닉네임 삽입 시 예외 발생")
    void member_nickname_unique_constraint() {
        testEm.persistAndFlush(
                Member.builder().kakaoId(11111L).nickname("동일닉네임").build());

        assertThrows(Exception.class, () ->
                testEm.persistAndFlush(
                        Member.builder().kakaoId(22222L).nickname("동일닉네임").build()));
    }

    @Test
    @DisplayName("Note MEDITATION UK — 동일 member+qtPassage+ACTIVE 중복 삽입 시 예외")
    void note_meditation_active_unique_constraint() {
        Note first = Note.builder()
                .memberId(1L)
                .qtPassageId(100L)
                .category(NoteCategory.MEDITATION)
                .title("묵상1")
                .body("본문1")
                .build();
        testEm.persistAndFlush(first);
        assertEquals("ACTIVE", first.getActiveUniqueKey());

        Note duplicate = Note.builder()
                .memberId(1L)
                .qtPassageId(100L)
                .category(NoteCategory.MEDITATION)
                .title("묵상2")
                .body("본문2")
                .build();

        assertThrows(Exception.class, () -> testEm.persistAndFlush(duplicate));
    }

    @Test
    @DisplayName("PostLike UK — 동일 sharingPostId+memberId 중복 삽입 시 예외")
    void postLike_unique_constraint() throws Exception {
        PostLike like1 = createPostLike(500L, 1L);
        testEm.persistAndFlush(like1);
        assertNotNull(like1.getId());

        PostLike like2 = createPostLike(500L, 1L);
        assertThrows(Exception.class, () -> testEm.persistAndFlush(like2));
    }

    /** PostLike는 protected 생성자만 존재 — 리플렉션으로 필드 세팅 */
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
}
