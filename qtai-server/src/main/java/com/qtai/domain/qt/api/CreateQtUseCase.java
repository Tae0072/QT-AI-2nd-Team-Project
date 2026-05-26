package com.qtai.domain.qt.api;

/**
 * QT 작성 UseCase 포트.
 *
 * QT(Quiet Time)는 회원이 작성하는 묵상 기록 — 본 서비스의 핵심 엔티티.
 */
public interface CreateQtUseCase {

    // TODO: QtResponse createQt(Long memberId, QtCreateRequest request);
    //       작성자 검증 → 성경 절 검증 → INSERT → QtResponse 반환
}
