package com.qtai.domain.audit.api;

import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

/**
 * 감사 로그 기록 UseCase 포트 (횡단 관심사).
 *
 * 예외 규칙: audit은 모든 도메인이 의존할 수 있는 횡단 관심사 —
 * client/ 어댑터 우회 없이 다른 도메인 Service가 직접 주입해 사용한다.
 * 현재 구현은 체크리스트 변경 감사에 필요한 최소 동기 저장 계약만 제공한다.
 */
public interface WriteAuditLogUseCase {

    void write(AuditLogWriteRequest request);
}
