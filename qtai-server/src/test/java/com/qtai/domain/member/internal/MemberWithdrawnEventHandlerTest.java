package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * MemberWithdrawnEventHandler 단위 테스트.
 */
class MemberWithdrawnEventHandlerTest {

    private RefreshTokenStore refreshTokenStore;
    private MemberWithdrawnEventHandler handler;

    @BeforeEach
    void setUp() {
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        handler = new MemberWithdrawnEventHandler(refreshTokenStore);
    }

    @Test
    void handle_탈퇴_이벤트_수신시_refresh_token_삭제() {
        handler.handle(new MemberWithdrawnEvent("evt-1", 1L));

        verify(refreshTokenStore).delete(1L);
    }

    @Test
    void handle_삭제_실패시_예외를_전파하지_않고_로그만_남긴다() {
        // refresh 갱신 경로가 WITHDRAWN을 차단하는 이중 방어가 있어 전파 불필요
        doThrow(new IllegalStateException("redis down"))
                .when(refreshTokenStore).delete(1L);

        assertThatCode(() -> handler.handle(new MemberWithdrawnEvent("evt-2", 1L)))
                .doesNotThrowAnyException();
    }
}
