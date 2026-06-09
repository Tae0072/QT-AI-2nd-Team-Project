package com.qtai.domain.admin.api;

/**
 * 콘텐츠 모더레이션 UseCase 포트.
 *
 * 신고된 QT/노트/공유 스냅샷을 검토하고 숨김·삭제·반려 처리한다.
 * 모든 액션은 AdminActionLog로 감사 기록.
 */
public interface ModerateContentUseCase {

    // TODO: void hideContent(Long adminId, ContentType type, Long contentId, String reason);
    // TODO: void deleteContent(Long adminId, ContentType type, Long contentId, String reason);
    // TODO: void dismissReport(Long adminId, Long reportId, String reason);  // 신고 반려
}
