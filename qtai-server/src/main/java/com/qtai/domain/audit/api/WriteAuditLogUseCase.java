package com.qtai.domain.audit.api;

/**
 * 감사 로그 기록 UseCase 포트 (횡단 관심사).
 *
 * 예외 규칙: audit은 모든 도메인이 의존할 수 있는 횡단 관심사 —
 * client/ 어댑터 우회 없이 다른 도메인 Service가 직접 주입해 사용한다.
 * 호출은 fire-and-forget 성격(@Async 권장) — 비즈니스 로직을 막지 않도록.
 */
public interface WriteAuditLogUseCase {

    // TODO: void write(AuditLogWriteRequest request);
    //       내부에서 actor / action / target / metadata를 받아 AuditLog로 영속화.
}
