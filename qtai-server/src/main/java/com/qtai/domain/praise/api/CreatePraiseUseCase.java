package com.qtai.domain.praise.api;

/**
 * 찬양 등록 UseCase 포트.
 *
 * 운영자(ADMIN) 큐레이션만 허용 — 일반 회원의 직접 등록·AI 자동 추천은 금지(v3.1).
 * 저작권 리스크 회피를 위해 가사·음원은 저장하지 않고 메타정보만 보관.
 */
public interface CreatePraiseUseCase {

    // TODO: PraiseResponse create(Long adminId, PraiseCreateRequest request);
    //       호출자 ROLE=ADMIN 검증 (컨트롤러 단 @PreAuthorize와 이중 방어)
}
