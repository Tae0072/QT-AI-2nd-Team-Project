package com.qtai.domain.sharing.internal;

/**
 * 나눔 게시글 엔티티 (ERD §2.15 sharing_posts).
 *
 * 노트 공개 시 원본 내용을 스냅샷 컬럼에 복사해 저장 (원자성 보장).
 * 공개 후 원본 노트가 수정·삭제되어도 게시글 내용은 자동 변경되지 않는다.
 * 닉네임은 공개 시점 값을 confirmNicknamePublic=true 확인 후 nickname_snapshot 에 저장.
 *
 * 원본 노트가 삭제·비공개 전환되면 source_note_deleted_at / source_note_unshared_at 기록 후
 * status 를 DELETED 또는 HIDDEN 으로 전환한다.
 */
// TODO: @Entity, @Table(name = "sharing_posts")
public class SharingPost {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @Column(name = "note_id", nullable = false, unique = true)
    //        Long noteId;                    — 원본 노트 FK/UK (1:1)

    // TODO: @Column(name = "member_id", nullable = false)
    //        Long memberId;                  — 작성자 FK

    // TODO: @Column(name = "nickname_snapshot", nullable = false, length = 30)
    //        String nicknameSnapshot;        — 공개 시점 닉네임

    // TODO: @Column(name = "note_category_snapshot", nullable = false, length = 30)
    //        String noteCategorySnapshot;    — 공유 시점 노트 카테고리

    // TODO: @Column(name = "title_snapshot", length = 100)
    //        String titleSnapshot;           — 제목 스냅샷 (nullable)

    // TODO: @Column(name = "feeling_snapshot", columnDefinition = "TEXT")
    //        String feelingSnapshot;         — QT 노트 '느낀 점' 스냅샷

    // TODO: @Column(name = "memory_verse_snapshot", columnDefinition = "TEXT")
    //        String memoryVerseSnapshot;

    // TODO: @Column(name = "application_snapshot", columnDefinition = "TEXT")
    //        String applicationSnapshot;

    // TODO: @Column(name = "prayer_snapshot", columnDefinition = "TEXT")
    //        String prayerSnapshot;

    // TODO: @Column(name = "body_snapshot", columnDefinition = "TEXT")
    //        String bodySnapshot;            — 자유 노트 본문 스냅샷

    // TODO: @Column(name = "verse_snapshot_json", columnDefinition = "JSON")
    //        String verseSnapshotJson;       — 공유 시점 선택 구절 목록 JSON

    // TODO: @Column(name = "comments_enabled", nullable = false)
    //        boolean commentsEnabled;        — 기본 true

    // TODO: @Enumerated(EnumType.STRING)
    //        @Column(nullable = false, length = 20)
    //        SharingPostStatus status;       — PUBLISHED, HIDDEN, DELETED

    // TODO: @Column(name = "source_note_deleted_at")
    //        LocalDateTime sourceNoteDeletedAt;   — 원본 노트 삭제 감지 시각

    // TODO: @Column(name = "source_note_unshared_at")
    //        LocalDateTime sourceNoteUnsharedAt;  — 원본 노트 비공개/공유취소 감지 시각

    // TODO: @Column(name = "published_at", nullable = false)
    //        @CreationTimestamp LocalDateTime publishedAt;

    // TODO: LocalDateTime hiddenAt;
    // TODO: @CreationTimestamp LocalDateTime createdAt;
    // TODO: @UpdateTimestamp  LocalDateTime updatedAt;

    // 연관
    // TODO: @OneToMany(mappedBy = "sharingPost", cascade = CascadeType.ALL) List<Comment> comments;
    // TODO: @OneToMany(mappedBy = "sharingPost", cascade = CascadeType.ALL) List<PostLike> likes;
}
