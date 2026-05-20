package com.qtai.domain.qt.internal;

/**
 * QT 도메인 진입점. 5개 UseCase 구현 + 트랜잭션 경계.
 *
 * 타 도메인 접근은 client/ 어댑터로만:
 *   - member.GetMemberUseCase     — 작성자 검증/닉네임
 *   - bible.GetBibleVerseUseCase  — 참조 절 검증/표시
 *   - ai.GenerateAiResponseUseCase — 선택적 AI 피드백 (호출 시점/플로우 정책 결정)
 *
 * 권한 정책:
 *   - 조회: PUBLIC이거나 본인 작성 QT만 (FORBIDDEN)
 *   - 수정/삭제: 작성자 본인만 (FORBIDDEN)
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreateQtUseCase, GetQtUseCase, ListMyQtUseCase, UpdateQtUseCase, DeleteQtUseCase
public class QtService {

    // TODO: final QtRepository qtRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetBibleVerseUseCase getBibleVerseUseCase;
    // TODO: final GenerateAiResponseUseCase generateAiResponseUseCase;

    // TODO: @Transactional createQt — 작성자/절 검증 → INSERT → QtResponse
    // TODO: getQt(viewerId, qtId) — 가시 권한 검사 후 반환
    // TODO: listMyQt(memberId, pageable) — 본인 작성만
    // TODO: @Transactional updateQt — 본인 검증 → 부분 업데이트 (null=유지)
    // TODO: @Transactional deleteQt — 본인 검증 → 삭제
}
