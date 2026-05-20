package com.qtai.domain.audit.internal;

/**
 * 감사 도메인 진입점. 2개 UseCase 구현 + 트랜잭션 경계.
 *
 * 횡단 관심사 정책: audit은 client/ 어댑터 없이 다른 도메인 Service가 직접 의존한다.
 * write()는 @Async로 비동기 처리해 호출자 응답 속도에 영향이 가지 않도록 한다.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements WriteAuditLogUseCase, ListAuditUseCase
// TODO: @EnableAsync 설정 + write 메서드에 @Async 부여
public class AuditService {

    // TODO: final AuditRepository auditRepository;

    // TODO: @Async @Transactional write(request) 구현
    //       1) request → AuditLog 엔티티 변환
    //       2) auditRepository.save(...)
    //       3) 예외 발생 시 호출자에게 전파 금지 (로그만 남기고 swallow)

    // TODO: list(actorId, action, from, to, pageable) 구현 — Page<AuditLogResponse> 반환
}
