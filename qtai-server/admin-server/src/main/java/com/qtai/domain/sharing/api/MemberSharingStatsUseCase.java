package com.qtai.domain.sharing.api;

import java.util.List;

/**
 * 회원 상세용 나눔 통계 UseCase 포트 (관리자 회원 관리에서 호출).
 *
 * <p>도메인 경계: sharing 내부를 노출하지 않고, 받은 신고 집계에 필요한 회원 소유 콘텐츠 ID와
 * 작성 공유글 수만 제공한다.
 */
public interface MemberSharingStatsUseCase {

    /** 회원이 작성한 공유글 수(상태 무관). */
    long countPostsByMember(Long memberId);

    /** 회원이 작성한 공유글 ID 목록(받은 신고 POST 집계용). */
    List<Long> listPostIdsByMember(Long memberId);

    /** 회원이 작성한 댓글 ID 목록(받은 신고 COMMENT 집계용). */
    List<Long> listCommentIdsByMember(Long memberId);
}
