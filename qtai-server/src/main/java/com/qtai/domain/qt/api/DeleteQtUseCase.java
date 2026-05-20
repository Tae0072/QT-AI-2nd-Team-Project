package com.qtai.domain.qt.api;

/**
 * QT 삭제 UseCase 포트.
 *
 * 작성자 본인만 삭제 가능. 위반 시 FORBIDDEN.
 * 정책: hard delete 또는 soft delete (deletedAt) — 결정 필요.
 */
public interface DeleteQtUseCase {

    // TODO: void deleteQt(Long memberId, Long qtId);
}
