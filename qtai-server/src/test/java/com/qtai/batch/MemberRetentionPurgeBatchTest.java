package com.qtai.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.annotation.Scheduled;

import com.qtai.domain.member.api.PurgeExpiredWithdrawnMembersUseCase;

/**
 * MemberRetentionPurgeBatch 회귀 테스트 — 스케줄(03:00 KST) 고정 검증.
 *
 * <p>00:05(해설 시딩)·04:00(사용자 노출 갱신) 고정 배치와 시간대가 겹치지 않아야
 * 하므로 cron 변경은 의도적 결정이어야 한다 (CLAUDE.md §6 고정 제품 결정 참조).
 */
class MemberRetentionPurgeBatchTest {

    @Test
    void purgeDaily_스케줄은_매일_03시_KST_고정() throws NoSuchMethodException {
        Method method = MemberRetentionPurgeBatch.class.getDeclaredMethod("purgeDaily");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 3 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void purgeDaily_UseCase에_위임하고_결과를_로깅한다() {
        PurgeExpiredWithdrawnMembersUseCase useCase =
                Mockito.mock(PurgeExpiredWithdrawnMembersUseCase.class);
        when(useCase.purgeExpired()).thenReturn(3);

        new MemberRetentionPurgeBatch(useCase).purgeDaily();

        verify(useCase).purgeExpired();
    }
}
