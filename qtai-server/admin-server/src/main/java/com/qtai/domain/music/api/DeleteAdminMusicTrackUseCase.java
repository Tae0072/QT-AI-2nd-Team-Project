package com.qtai.domain.music.api;

/**
 * 관리자 배경음악 삭제 UseCase 포트.
 *
 * <p>소프트 삭제(deletedAt 기록 + 노출 비활성화)로 처리해 목록·스트리밍에서 제외한다.
 */
public interface DeleteAdminMusicTrackUseCase {

    void deleteAdmin(Long adminUserId, Long trackId);
}
