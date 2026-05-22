package com.qtai.common;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.domain.member.internal.Member;
import com.qtai.domain.member.internal.MemberStatus;
import com.qtai.domain.note.internal.Note;
import com.qtai.domain.note.internal.NoteVerse;
import com.qtai.domain.qt.internal.QtPassage;
import com.qtai.domain.qt.internal.QtPassageVerse;
import com.qtai.domain.sharing.internal.Comment;
import com.qtai.domain.sharing.internal.PostLike;
import com.qtai.domain.sharing.internal.SharingPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 공통 기반 — Entity 클래스 로드·리플렉션 인스턴스 생성 검증.
 * protected 기본 생성자를 가진 Entity도 리플렉션으로 검증.
 */
class EntityCompilationTest {

    private <T> T newInstance(Class<T> clazz) throws Exception {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Test
    @DisplayName("BaseEntity 서브클래스들이 리플렉션으로 정상 인스턴스화된다")
    void baseEntity_subclasses_instantiate() throws Exception {
        assertNotNull(newInstance(QtPassage.class));
        assertNotNull(newInstance(Note.class));
        assertNotNull(newInstance(SharingPost.class));
        assertNotNull(newInstance(Comment.class));
    }

    @Test
    @DisplayName("BibleBook, BibleVerse — 고정 PK 엔티티 인스턴스화")
    void bibleEntities_instantiate() throws Exception {
        assertNotNull(newInstance(BibleBook.class));
        assertNotNull(newInstance(BibleVerse.class));
    }

    @Test
    @DisplayName("Member — Builder 패턴으로 생성 가능하다")
    void member_builder_works() {
        Member member = Member.builder()
                .kakaoId(12345L)
                .nickname("테스트유저")
                .build();
        assertEquals(12345L, member.getKakaoId());
        assertEquals("테스트유저", member.getNickname());
    }

    @Test
    @DisplayName("MemberStatus — enum 값이 3종 존재한다")
    void memberStatus_has_three_values() {
        MemberStatus[] values = MemberStatus.values();
        assertEquals(3, values.length);
        assertNotNull(MemberStatus.ACTIVE);
        assertNotNull(MemberStatus.SUSPENDED);
        assertNotNull(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("NoteVerse, QtPassageVerse, PostLike 인스턴스화")
    void joinEntities_instantiate() throws Exception {
        assertNotNull(newInstance(NoteVerse.class));
        assertNotNull(newInstance(QtPassageVerse.class));
        assertNotNull(newInstance(PostLike.class));
    }
}
