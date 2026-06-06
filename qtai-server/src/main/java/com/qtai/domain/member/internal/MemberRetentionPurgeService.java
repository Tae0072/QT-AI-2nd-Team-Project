package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.member.api.PurgeExpiredWithdrawnMembersUseCase;
import com.qtai.domain.mission.api.PurgeMemberMissionDataUseCase;
import com.qtai.domain.note.api.PurgeMemberNoteDataUseCase;
import com.qtai.domain.notification.api.PurgeMemberNotificationDataUseCase;
import com.qtai.domain.praise.api.PurgeMemberPraiseDataUseCase;
import com.qtai.domain.report.api.PurgeMemberReportDataUseCase;
import com.qtai.domain.sharing.api.PurgeMemberSharingDataUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 보존기간(2년) 만료 탈퇴 회원 정리 오케스트레이터.
 *
 * <p>정책(2026-06-05 결정): 탈퇴 회원의 개인정보·작성 기록은 2년 보관 후
 * 전부 hard delete한다. 탈퇴자 나눔 글에 달린 다른 회원의 댓글·좋아요도
 * 글과 함께 연쇄 삭제된다.
 *
 * <p>도메인 경계: 각 도메인 데이터 삭제는 해당 도메인의 {@code api} 포트
 * (Purge*UseCase)를 통해 위임하고, 이 서비스는 member 도메인 소유 테이블
 * (members, member_settings)만 직접 삭제한다. 관리자 연결 확인도
 * {@link VerifyAdminRoleUseCase} 포트로 위임한다.
 *
 * <p>호출 순서는 FK 제약 역순으로 고정한다:
 * sharing(sharing_posts.note_id → notes 선행 해제) → note → praise → mission
 * → notification → report → member 본체(member_auth_providers는 ON DELETE CASCADE).
 *
 * <p>회원 1명 = 트랜잭션 1개로 처리해 일부 실패가 전체를 롤백하지 않게 하고,
 * 1회 실행당 {@value #BATCH_LIMIT}명까지만 처리한다(대량 만료 시 부하 제한 —
 * 잔여분은 다음 실행에서 이어서 처리).
 */
@Slf4j
@Service
public class MemberRetentionPurgeService implements PurgeExpiredWithdrawnMembersUseCase {

    private static final int RETENTION_YEARS = 2;
    /** 1회 실행당 최대 처리 인원 — 일 1회 배치 기준 충분, 잔여분은 다음 실행에서 처리. */
    private static final int BATCH_LIMIT = 500;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final PurgeMemberSharingDataUseCase purgeSharing;
    private final PurgeMemberNoteDataUseCase purgeNote;
    private final PurgeMemberPraiseDataUseCase purgePraise;
    private final PurgeMemberMissionDataUseCase purgeMission;
    private final PurgeMemberNotificationDataUseCase purgeNotification;
    private final PurgeMemberReportDataUseCase purgeReport;

    public MemberRetentionPurgeService(
            JdbcTemplate jdbc,
            TransactionTemplate transactionTemplate,
            Clock clock,
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            PurgeMemberSharingDataUseCase purgeSharing,
            PurgeMemberNoteDataUseCase purgeNote,
            PurgeMemberPraiseDataUseCase purgePraise,
            PurgeMemberMissionDataUseCase purgeMission,
            PurgeMemberNotificationDataUseCase purgeNotification,
            PurgeMemberReportDataUseCase purgeReport
    ) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
        this.purgeSharing = purgeSharing;
        this.purgeNote = purgeNote;
        this.purgePraise = purgePraise;
        this.purgeMission = purgeMission;
        this.purgeNotification = purgeNotification;
        this.purgeReport = purgeReport;
    }

    @Override
    public int purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusYears(RETENTION_YEARS);
        List<Long> targets = jdbc.queryForList(
                "SELECT id FROM members WHERE status = 'WITHDRAWN' AND withdrawn_at <= ? "
                        + "ORDER BY withdrawn_at ASC LIMIT " + BATCH_LIMIT,
                Long.class, cutoff);

        if (targets.isEmpty()) {
            return 0;
        }

        int purged = 0;
        for (Long memberId : targets) {
            try {
                if (hasAdminLink(memberId)) {
                    // admin_users는 reports.processed_by_admin_id가 참조하므로 자동 삭제하지
                    // 않는다 — 운영자 계정 정리는 수동 처리 대상으로 남긴다.
                    log.warn("[retention] 관리자 연결 회원은 자동 삭제 제외(수동 처리 필요): memberId={}", memberId);
                    continue;
                }
                transactionTemplate.executeWithoutResult(status -> purgeOne(memberId));
                purged++;
                log.info("[retention] 보존기간 만료 회원 삭제 완료: memberId={}", memberId);
            } catch (DataAccessException | TransactionException | IllegalStateException e) {
                // 한 회원의 영속성/트랜잭션/데이터 이상(댓글 cycle 등) 실패가
                // 배치 전체를 중단하지 않도록 기록 후 다음 회원을 계속 처리한다.
                log.error("[retention] 회원 삭제 실패 — 다음 회원 계속: memberId={}", memberId, e);
            }
        }
        return purged;
    }

    /**
     * 관리자 연결 여부 — admin 도메인 포트로 확인한다.
     * ADMIN_USER_NOT_FOUND만 "연결 없음"으로 간주하고, 비활성(DISABLED) 등
     * row가 존재하는 경우는 모두 연결로 간주해 자동 삭제에서 제외한다.
     */
    private boolean hasAdminLink(Long memberId) {
        try {
            verifyAdminRoleUseCase.getActiveAdmin(memberId);
            return true;
        } catch (BusinessException e) {
            return e.getErrorCode() != ErrorCode.ADMIN_USER_NOT_FOUND;
        }
    }

    /**
     * 회원 1명의 데이터를 FK 역순으로 삭제한다 (단일 트랜잭션 안에서 호출).
     */
    private void purgeOne(Long memberId) {
        // 1~2) 타 도메인 데이터 — 각 도메인 포트로 위임 (sharing이 notes FK를 먼저 해제)
        purgeSharing.purgeByMemberId(memberId);
        purgeNote.purgeByMemberId(memberId);
        // 3~6) 회원 부속 도메인 데이터
        purgePraise.purgeByMemberId(memberId);
        purgeMission.purgeByMemberId(memberId);
        purgeNotification.purgeByMemberId(memberId);
        purgeReport.purgeByMemberId(memberId);
        // 7) member 도메인 소유 테이블
        jdbc.update("DELETE FROM member_settings WHERE member_id = ?", memberId);
        // 8) 회원 본체 — member_auth_providers는 FK ON DELETE CASCADE로 함께 삭제
        jdbc.update("DELETE FROM members WHERE id = ?", memberId);
    }
}
