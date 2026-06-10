package com.qtai.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.qtai.domain.member.api.PurgeExpiredWithdrawnMembersUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link MemberRetentionPurgeBatch}의 활성화 게이트({@code qtai.retention.purge.enabled}) 검증.
 *
 * <p>hard delete 배치라 기본 off — 속성이 true일 때만 빈이 등록돼 매일 03:00 스케줄이 동작한다.
 * 속성 미설정/false면 빈이 없어 자동 정리가 돌지 않는다(개발/테스트 데이터 보호).
 */
class MemberRetentionPurgeBatchGateTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(PurgeExpiredWithdrawnMembersUseCase.class,
                    () -> mock(PurgeExpiredWithdrawnMembersUseCase.class))
            .withConfiguration(UserConfigurations.of(MemberRetentionPurgeBatch.class));

    @Test
    @DisplayName("enabled=true이면 배치 빈이 등록된다")
    void enabledTrue_배치등록() {
        runner.withPropertyValues("qtai.retention.purge.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MemberRetentionPurgeBatch.class));
    }

    @Test
    @DisplayName("enabled=false이면 배치 빈이 없다")
    void enabledFalse_배치미등록() {
        runner.withPropertyValues("qtai.retention.purge.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MemberRetentionPurgeBatch.class));
    }

    @Test
    @DisplayName("속성 미설정(기본)이면 배치 빈이 없다 — 안전 기본값 off")
    void 미설정_배치미등록() {
        runner.run(context -> assertThat(context).doesNotHaveBean(MemberRetentionPurgeBatch.class));
    }
}
