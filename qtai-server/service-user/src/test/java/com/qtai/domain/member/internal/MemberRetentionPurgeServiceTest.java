package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.mission.api.PurgeMemberMissionDataUseCase;
import com.qtai.domain.note.api.PurgeMemberNoteDataUseCase;
import com.qtai.domain.notification.api.PurgeMemberNotificationDataUseCase;
import com.qtai.domain.praise.api.PurgeMemberPraiseDataUseCase;
import com.qtai.domain.report.api.PurgeMemberReportDataUseCase;
import com.qtai.domain.sharing.api.PurgeMemberSharingDataUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link MemberRetentionPurgeService} 단위 테스트 — deploy guard / FK 역순 위임 / 관리자 제외.
 *
 * <p>위험도 높은 보존기간 만료 hard-delete 배치라 가드와 admin 연결 분기를 명시적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberRetentionPurgeServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    @Mock private PurgeMemberSharingDataUseCase purgeSharing;
    @Mock private PurgeMemberNoteDataUseCase purgeNote;
    @Mock private PurgeMemberPraiseDataUseCase purgePraise;
    @Mock private PurgeMemberMissionDataUseCase purgeMission;
    @Mock private PurgeMemberNotificationDataUseCase purgeNotification;
    @Mock private PurgeMemberReportDataUseCase purgeReport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-10T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemberRetentionPurgeService service(boolean enabled) {
        return new MemberRetentionPurgeService(enabled, jdbc, transactionTemplate, clock,
                verifyAdminRoleUseCase, purgeSharing, purgeNote, purgePraise, purgeMission,
                purgeNotification, purgeReport);
    }

    /** TransactionTemplate.executeWithoutResult(consumer)가 콜백을 실제로 실행하도록 한다. */
    private void runConsumerInline() {
        doAnswer(inv -> {
            Consumer<TransactionStatus> c = inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void 가드가_꺼져있으면_아무것도_하지_않는다() {
        int purged = service(false).purgeExpired();

        assertThat(purged).isZero();
        verify(jdbc, never()).queryForList(anyString(), eq(Long.class), any());
    }

    @Test
    void 대상이_없으면_0을_반환한다() {
        when(jdbc.queryForList(anyString(), eq(Long.class), any())).thenReturn(List.of());

        assertThat(service(true).purgeExpired()).isZero();
    }

    @Test
    void 관리자가_아니면_FK_역순으로_삭제를_위임한다() {
        when(jdbc.queryForList(anyString(), eq(Long.class), any())).thenReturn(List.of(1L));
        when(verifyAdminRoleUseCase.getActiveAdmin(1L))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));
        runConsumerInline();

        int purged = service(true).purgeExpired();

        assertThat(purged).isEqualTo(1);
        // sharing이 note FK를 먼저 해제해야 하므로 타 도메인 위임이 모두 호출돼야 한다.
        verify(purgeSharing).purgeByMemberId(1L);
        verify(purgeNote).purgeByMemberId(1L);
        verify(purgePraise).purgeByMemberId(1L);
        verify(purgeMission).purgeByMemberId(1L);
        verify(purgeNotification).purgeByMemberId(1L);
        verify(purgeReport).purgeByMemberId(1L);
        verify(jdbc).update(contains("member_settings"), eq(1L));
        verify(jdbc).update(contains("members"), eq(1L));
    }

    @Test
    void 관리자_연결_회원은_자동삭제에서_제외한다() {
        when(jdbc.queryForList(anyString(), eq(Long.class), any())).thenReturn(List.of(1L));
        when(verifyAdminRoleUseCase.getActiveAdmin(1L))
                .thenReturn(new AdminUserInfo(9L, 1L, "OPERATOR"));

        int purged = service(true).purgeExpired();

        assertThat(purged).isZero();
        verify(purgeSharing, never()).purgeByMemberId(any());
    }
}
