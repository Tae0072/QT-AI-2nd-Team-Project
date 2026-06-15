package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.AdminMemberCommentItem;
import com.qtai.domain.sharing.api.dto.AdminMemberLikedPostItem;
import com.qtai.domain.sharing.api.dto.AdminMemberPostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 회원 상세 — 회원의 나눔 활동(공유글/댓글/좋아요) 조회 UseCase 포트.
 *
 * <p>도메인 경계: member 도메인이 sharing 내부를 직접 호출하지 않고 이 포트로만 본다.
 */
public interface AdminMemberSharingQueryUseCase {

    /** 회원이 공유한 나눔글(전체 상태)을 최신순으로 페이징 조회한다. */
    Page<AdminMemberPostItem> listPostsByMember(Long memberId, Pageable pageable);

    /** 회원이 작성한 댓글(삭제 포함, 운영 식별용)을 최신순으로 페이징 조회한다. */
    Page<AdminMemberCommentItem> listCommentsByMember(Long memberId, Pageable pageable);

    /** 회원이 좋아요한 나눔글을 좋아요 누른 시각 최신순으로 페이징 조회한다. */
    Page<AdminMemberLikedPostItem> listLikedPostsByMember(Long memberId, Pageable pageable);
}
