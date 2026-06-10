package com.qtai.domain.member.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 탈퇴 이벤트 핸들러 — 탈퇴 트랜잭션 커밋 후 세션(refresh token) 무효화.
 *
 * <p>AFTER_COMMIT으로 분리한 이유: 탈퇴 트랜잭션이 롤백되면 토큰도 유지되어야
 * 하고, Redis 호출 실패가 탈퇴 트랜잭션을 깨지 않아야 한다.
 *
 * <p>핸들러 실패 시 로그를 남기고 전파하지 않는다 — refresh 갱신 경로가
 * WITHDRAWN 상태를 확인해 토큰을 폐기하므로 이중 방어가 존재한다(재처리 가능 상태).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MemberWithdrawnEventHandler {

    private static final String HANDLER_NAME = "MemberWithdrawnEventHandler";

    private final RefreshTokenStore refreshTokenStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberWithdrawnEvent event) {
        try {
            refreshTokenStore.delete(event.memberId());
            log.info("탈퇴 세션 무효화 완료: memberId={}", event.memberId());
        } catch (RuntimeException e) {
            log.error("member withdrawn event handler failed: eventId={}, eventType={}, "
                            + "handlerName={}, errorMessage={}",
                    event.eventId(), "MemberWithdrawnEvent", HANDLER_NAME, e.getMessage(), e);
        }
    }
}
