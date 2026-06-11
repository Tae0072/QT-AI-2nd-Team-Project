package com.qtai.domain.praise.api;

/**
 * 찬양 큐레이션 곡 삭제 UseCase 포트 (ADMIN only).
 */
public interface DeletePraiseUseCase {

    void delete(Long adminId, Long praiseSongId);
}
