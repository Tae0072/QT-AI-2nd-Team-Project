package com.qtai.domain.qt.api;

/**
 * QT 조회 UseCase 포트.
 *
 * 호출자: note / sharing / mission / ai 도메인. 가시 권한 정책:
 *   - PUBLIC QT는 누구나 조회 가능
 *   - PRIVATE QT는 작성자(viewerId == authorId)만 조회 가능
 *   - 위반 시 FORBIDDEN
 */
public interface GetQtUseCase {

    // TODO: QtResponse getQt(Long viewerId, Long qtId);
}
