package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.mission.internal.MissionPurgeService;
import com.qtai.domain.note.internal.NotePurgeService;
import com.qtai.domain.notification.internal.NotificationPurgeService;
import com.qtai.domain.praise.internal.PraisePurgeService;
import com.qtai.domain.report.internal.ReportPurgeService;
import com.qtai.domain.sharing.internal.SharingPurgeService;

/**
 * MemberRetentionPurgeService(오케스트레이터) 통합 테스트 (H2).
 *
 * <p>탈퇴 후 2년(보존기간) 경과 회원의 hard delete 정책(2026-06-05 결정) 검증.
 * 도메인 포트(Purge*UseCase)는 실제 구현으로 wiring하여 도메인 간 삭제 순서까지
 * 함께 검증한다 (테스트 코드는 ArchUnit 경계 검사 제외 — DoNotIncludeTests).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MemberRetentionPurgeServiceTest {

    /** 고정 현재 시각: 2026-06-05T12:00 KST → cutoff = 2024-06-05T12:00. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private MemberRetentionPurgeService service;

    @BeforeEach
    void setUp() {
        // 기본: 관리자 연결 없음 (ADMIN_USER_NOT_FOUND)
        verifyAdminRoleUseCase = Mockito.mock(VerifyAdminRoleUseCase.class);
        when(verifyAdminRoleUseCase.getActiveAdmin(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));

        service = new MemberRetentionPurgeService(
                jdbc, new TransactionTemplate(txManager), FIXED_CLOCK,
                verifyAdminRoleUseCase,
                new SharingPurgeService(jdbc),
                new NotePurgeService(jdbc),
                new PraisePurgeService(jdbc),
                new MissionPurgeService(jdbc),
                new NotificationPurgeService(jdbc),
                new ReportPurgeService(jdbc));
    }

    @Test
    void purgeExpired_2년경과_회원과_콘텐츠_연쇄삭제_나머지_보존() {
        // given — A: 25개월 전 탈퇴(삭제 대상), B: 활성, C: 1개월 전 탈퇴(보존)
        long a = insertMember("expiredA", 101L, "WITHDRAWN", LocalDateTime.parse("2024-05-05T10:00:00"));
        long b = insertMember("activeB", 102L, "ACTIVE", null);
        long c = insertMember("recentC", 103L, "WITHDRAWN", LocalDateTime.parse("2026-05-05T10:00:00"));

        long noteA = insertNote(a, "A의 노트");
        long noteB = insertNote(b, "B의 노트");
        long postA = insertPost(a, noteA, "A의 나눔");
        long postB = insertPost(b, noteB, "B의 나눔");

        // 댓글 그래프: B가 A 글에 댓글(c1), A가 B 글에 댓글(c2), B가 c2에 대댓글(r1),
        // c2 트리 3단계 — r1에 대댓글(r2, 타인), B가 자기 글에 댓글(c3 — 보존 대상)
        long c1 = insertComment(postA, b, null, "B가 A글에");
        long c2 = insertComment(postB, a, null, "A가 B글에");
        long r1 = insertComment(postB, b, c2, "B가 A댓글에 대댓글");
        long r2 = insertComment(postB, b, r1, "3단계 대댓글");
        long c3 = insertComment(postB, b, null, "B가 자기 글에");

        // 좋아요: A→B글, B→A글(글 따라 삭제), B→B글(보존)
        insertLike(postB, a);
        insertLike(postA, b);
        insertLike(postB, b);

        // when
        int purged = service.purgeExpired();

        // then — A만 삭제
        assertThat(purged).isEqualTo(1);
        assertThat(countWhere("members", "id = " + a)).isZero();
        assertThat(countWhere("members", "id = " + b)).isOne();
        assertThat(countWhere("members", "id = " + c)).isOne();

        // A의 콘텐츠 + A 글의 타인 댓글/좋아요 + A 댓글의 대댓글 트리(3단계 포함) 삭제
        assertThat(countWhere("notes", "id = " + noteA)).isZero();
        assertThat(countWhere("sharing_posts", "id = " + postA)).isZero();
        assertThat(countWhere("comments",
                "id IN (" + c1 + "," + c2 + "," + r1 + "," + r2 + ")")).isZero();
        assertThat(countWhere("post_likes", "member_id = " + a)).isZero();
        assertThat(countWhere("post_likes", "sharing_post_id = " + postA)).isZero();

        // B의 콘텐츠는 보존
        assertThat(countWhere("notes", "id = " + noteB)).isOne();
        assertThat(countWhere("sharing_posts", "id = " + postB)).isOne();
        assertThat(countWhere("comments", "id = " + c3)).isOne();
        assertThat(countWhere("post_likes", "sharing_post_id = " + postB + " AND member_id = " + b)).isOne();
    }

    @Test
    void purgeExpired_정확히_2년_경계는_삭제대상() {
        long d = insertMember("boundaryD", 104L, "WITHDRAWN", LocalDateTime.parse("2024-06-05T12:00:00"));

        int purged = service.purgeExpired();

        assertThat(purged).isEqualTo(1);
        assertThat(countWhere("members", "id = " + d)).isZero();
    }

    @Test
    void purgeExpired_관리자_연결_회원은_자동삭제_제외() {
        long e = insertMember("adminE", 105L, "WITHDRAWN", LocalDateTime.parse("2023-01-01T00:00:00"));
        Mockito.reset(verifyAdminRoleUseCase);
        when(verifyAdminRoleUseCase.getActiveAdmin(e)).thenReturn(null); // 연결 존재

        int purged = service.purgeExpired();

        assertThat(purged).isZero();
        assertThat(countWhere("members", "id = " + e)).isOne();
    }

    @Test
    void purgeExpired_댓글_cycle_데이터는_해당_회원만_건너뛰고_계속() {
        // given — F: 자기 글에 댓글 2개가 서로 부모(cycle, FK상 update로만 만들 수 있는 이상 데이터)
        long f = insertMember("cycleF", 106L, "WITHDRAWN", LocalDateTime.parse("2023-01-01T00:00:00"));
        long noteF = insertNote(f, "F의 노트");
        long postF = insertPost(f, noteF, "F의 나눔");
        long x = insertComment(postF, f, null, "cycle-x");
        long y = insertComment(postF, f, x, "cycle-y");
        jdbc.update("UPDATE comments SET parent_id = ? WHERE id = ?", y, x);

        // 정상 만료 회원 G도 함께 — cycle 실패가 배치 전체를 막지 않는지 검증
        long g = insertMember("normalG", 107L, "WITHDRAWN", LocalDateTime.parse("2023-06-01T00:00:00"));

        // when — 예외가 전파되지 않아야 한다
        int purged = service.purgeExpired();

        // then — G만 삭제, F는 보존(수동 처리 대상으로 로그 기록)
        assertThat(purged).isEqualTo(1);
        assertThat(countWhere("members", "id = " + g)).isZero();
        assertThat(countWhere("members", "id = " + f)).isOne();
        assertThat(countWhere("comments", "id IN (" + x + "," + y + ")")).isEqualTo(2);
    }

    @Test
    void purgeExpired_대상없음_0건() {
        insertMember("onlyActive", 108L, "ACTIVE", null);

        assertThat(service.purgeExpired()).isZero();
    }

    // ── insert helpers (H2 스키마는 엔티티 기준 생성) ──

    private long insertMember(String nickname, Long kakaoId, String status, LocalDateTime withdrawnAt) {
        jdbc.update("INSERT INTO members (kakao_id, nickname, role, status, withdrawn_at, created_at, updated_at) "
                + "VALUES (?, ?, 'USER', ?, ?, NOW(), NOW())", kakaoId, nickname, status, withdrawnAt);
        return jdbc.queryForObject("SELECT id FROM members WHERE nickname = ?", Long.class, nickname);
    }

    private long insertNote(long memberId, String title) {
        jdbc.update("INSERT INTO notes (member_id, category, status, visibility, title, body, created_at, updated_at) "
                + "VALUES (?, 'MEDITATION', 'SAVED', 'PRIVATE', ?, '본문', NOW(), NOW())", memberId, title);
        return jdbc.queryForObject("SELECT id FROM notes WHERE title = ?", Long.class, title);
    }

    private long insertPost(long memberId, long noteId, String title) {
        jdbc.update("INSERT INTO sharing_posts (member_id, note_id, status, snapshot_title, snapshot_body, "
                + "snapshot_category, comments_enabled, like_count, comment_count, created_at, updated_at) "
                + "VALUES (?, ?, 'PUBLISHED', ?, '본문', 'MEDITATION', true, 0, 0, NOW(), NOW())",
                memberId, noteId, title);
        return jdbc.queryForObject("SELECT id FROM sharing_posts WHERE snapshot_title = ?", Long.class, title);
    }

    private long insertComment(long postId, long memberId, Long parentId, String body) {
        jdbc.update("INSERT INTO comments (sharing_post_id, member_id, parent_id, body, is_deleted, "
                + "created_at, updated_at) VALUES (?, ?, ?, ?, false, NOW(), NOW())",
                postId, memberId, parentId, body);
        return jdbc.queryForObject("SELECT id FROM comments WHERE body = ?", Long.class, body);
    }

    private void insertLike(long postId, long memberId) {
        jdbc.update("INSERT INTO post_likes (sharing_post_id, member_id, created_at) VALUES (?, ?, NOW())",
                postId, memberId);
    }

    private int countWhere(String table, String where) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
        return count == null ? 0 : count;
    }
}
