package com.qtai.domain.member.internal;

import com.qtai.domain.member.api.PurgeExpiredWithdrawnMembersUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 보존기간(2년) 만료 탈퇴 회원 정리 서비스.
 *
 * <p>정책(2026-06-05 Lead 결정): 탈퇴 회원의 개인정보·작성 기록은 2년 보관 후
 * 전부 hard delete한다. 탈퇴자 나눔 글에 달린 다른 회원의 댓글·좋아요도
 * 글과 함께 연쇄 삭제된다.
 *
 * <p>구현 노트: 삭제 대상이 스키마 전반(12개 테이블)에 걸친 데이터 수명주기(retention)
 * 작업이라, 도메인별 UseCase 호출 대신 이 서비스 한 곳에서 JdbcTemplate SQL로
 * FK 역순 삭제한다. 다른 도메인의 Java 타입(Entity/Service/Repository)을 import하지
 * 않으므로 모듈 경계(import 기준)는 유지되며, 테이블 의존은 주석으로 명시한다.
 * 회원 1명 = 트랜잭션 1개로 처리해 일부 실패가 전체를 롤백하지 않게 한다.
 *
 * <p>대상 테이블(삭제 순서대로): journal_events, note_verses, post_likes, comments,
 * sharing_posts, notes, member_praise_songs, member_mission_progress, member_settings,
 * notifications, reports(신고자 기준), members(member_auth_providers는 ON DELETE CASCADE).
 */
@Slf4j
@Service
public class MemberRetentionPurgeService implements PurgeExpiredWithdrawnMembersUseCase {

    private static final int RETENTION_YEARS = 2;
    private static final int MAX_DELETE_PASSES = 50;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public MemberRetentionPurgeService(
            JdbcTemplate jdbc,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public int purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusYears(RETENTION_YEARS);
        List<Long> targets = jdbc.queryForList(
                "SELECT id FROM members WHERE status = 'WITHDRAWN' AND withdrawn_at <= ?",
                Long.class, cutoff);

        if (targets.isEmpty()) {
            return 0;
        }

        int purged = 0;
        for (Long memberId : targets) {
            try {
                if (isAdminMember(memberId)) {
                    // admin_users는 reports.processed_by_admin_id가 참조하므로 자동 삭제하지
                    // 않는다 — 운영자 계정 정리는 수동 처리 대상으로 남긴다.
                    log.warn("[retention] 관리자 연결 회원은 자동 삭제 제외(수동 처리 필요): memberId={}", memberId);
                    continue;
                }
                transactionTemplate.executeWithoutResult(status -> purgeOne(memberId));
                purged++;
                log.info("[retention] 보존기간 만료 회원 삭제 완료: memberId={}", memberId);
            } catch (Exception e) {
                // 한 회원 실패가 배치 전체를 중단하지 않도록 기록 후 계속 진행한다.
                log.error("[retention] 회원 삭제 실패 — 다음 회원 계속: memberId={}", memberId, e);
            }
        }
        return purged;
    }

    private boolean isAdminMember(Long memberId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE member_id = ?", Integer.class, memberId);
        return count != null && count > 0;
    }

    /**
     * 회원 1명의 데이터를 FK 역순으로 삭제한다 (단일 트랜잭션 안에서 호출).
     */
    private void purgeOne(Long memberId) {
        // 1) 묵상 일지 이벤트 — 회원 본인 것 + 회원 노트를 참조하는 것
        jdbc.update("DELETE FROM journal_events WHERE member_id = ? "
                + "OR note_id IN (SELECT id FROM notes WHERE member_id = ?)", memberId, memberId);

        // 2) 노트-구절 연결
        jdbc.update("DELETE FROM note_verses WHERE note_id IN "
                + "(SELECT id FROM notes WHERE member_id = ?)", memberId);

        // 3) 좋아요 — 회원이 누른 것 + 회원 나눔 글에 달린 것(타인 포함)
        jdbc.update("DELETE FROM post_likes WHERE member_id = ? "
                + "OR sharing_post_id IN (SELECT id FROM sharing_posts WHERE member_id = ?)",
                memberId, memberId);

        // 4) 댓글 — 회원 댓글 + 회원 글의 댓글 트리(타인 댓글 포함)를 자식부터 삭제
        deleteCommentTrees(memberId);

        // 5) 나눔 글 (댓글·좋아요 제거 후)
        jdbc.update("DELETE FROM sharing_posts WHERE member_id = ?", memberId);

        // 6) 노트 (note_verses, journal_events, sharing_posts 참조 제거 후)
        jdbc.update("DELETE FROM notes WHERE member_id = ?", memberId);

        // 7~10) 회원 부속 데이터
        jdbc.update("DELETE FROM member_praise_songs WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM member_mission_progress WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM member_settings WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM notifications WHERE member_id = ?", memberId);

        // 11) 회원이 접수한 신고 (타인이 접수한 신고는 접수자의 기록이므로 유지)
        jdbc.update("DELETE FROM reports WHERE reporter_member_id = ?", memberId);

        // 12) 회원 본체 — member_auth_providers는 FK ON DELETE CASCADE로 함께 삭제
        jdbc.update("DELETE FROM members WHERE id = ?", memberId);
    }

    /**
     * 삭제 대상 댓글(회원 본인 댓글 + 회원 글의 모든 댓글)과 그 대댓글 트리 전체를
     * 리프(자식 없는 댓글)부터 반복 삭제한다.
     *
     * <p>comments.parent_id 자기참조 FK 때문에 자식이 남아있는 댓글을 먼저 지울 수
     * 없고, 타인이 단 대댓글까지 따라가야 하므로 ① 대상 트리를 Java에서 transitive하게
     * 수집한 뒤 ② 매 패스마다 "아직 자식이 남은 id"를 제외한 리프만 삭제한다.
     * MySQL은 한 DELETE 문 안에서도 행 단위로 FK를 검사하므로 부모·자식을 같은
     * 문장에 섞으면 위반 가능성이 있다 (MySQL/H2 공통 동작 보장 방식).
     */
    private void deleteCommentTrees(Long memberId) {
        // ① 삭제 대상 수집: 본인 댓글 + 본인 글의 댓글 → 그 대댓글을 레벨 단위로 확장
        List<Long> doomed = jdbc.queryForList(
                "SELECT id FROM comments WHERE member_id = ? "
                        + "OR sharing_post_id IN (SELECT id FROM sharing_posts WHERE member_id = ?)",
                Long.class, memberId, memberId);
        if (doomed.isEmpty()) {
            return;
        }
        List<Long> frontier = doomed;
        while (!frontier.isEmpty()) {
            List<Long> children = jdbc.queryForList(
                    "SELECT id FROM comments WHERE parent_id IN (" + placeholders(frontier.size()) + ")",
                    Long.class, frontier.toArray());
            children.removeIf(doomed::contains);
            if (children.isEmpty()) {
                break;
            }
            doomed.addAll(children);
            frontier = children;
        }

        // ② 리프부터 반복 삭제 — 수집이 transitive하므로 매 패스 리프가 반드시 존재한다
        List<Long> remaining = new ArrayList<>(doomed);
        int guard = 0;
        while (!remaining.isEmpty()) {
            if (++guard > MAX_DELETE_PASSES) {
                throw new IllegalStateException(
                        "댓글 트리 삭제 반복 한도 초과 — 데이터 확인 필요: memberId=" + memberId);
            }
            List<Long> stillParents = jdbc.queryForList(
                    "SELECT DISTINCT parent_id FROM comments WHERE parent_id IN ("
                            + placeholders(remaining.size()) + ")",
                    Long.class, remaining.toArray());
            List<Long> leaves = new ArrayList<>(remaining);
            leaves.removeAll(stillParents);
            jdbc.update("DELETE FROM comments WHERE id IN (" + placeholders(leaves.size()) + ")",
                    leaves.toArray());
            remaining.retainAll(stillParents);
        }
    }

    private String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }
}
