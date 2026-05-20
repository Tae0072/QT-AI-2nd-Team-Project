package com.qtai.domain.praise.internal;

/**
 * 찬양 도메인 진입점. 2개 UseCase 구현 + 트랜잭션 경계.
 *
 * 운영 정책 (v3.1):
 *   - 등록은 ADMIN만 (큐레이션)
 *   - AI 기반 찬양 자동 추천/생성 기능 금지
 *   - 가사·음원 본문 저장 금지 (externalLink만)
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreatePraiseUseCase, ListPraiseUseCase
public class PraiseService {

    // TODO: final PraiseRepository praiseRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;

    // TODO: @Transactional create(adminId, request) — 권한 검증 후 INSERT
    // TODO: list(keyword, pageable) 구현 — 키워드가 null이면 findAll
}
