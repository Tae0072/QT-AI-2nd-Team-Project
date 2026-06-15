package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 나눔 공유글 운영 UseCase 포트 (F-10, AD-15).
 *
 * <p>admin-server 고유 기능. 사용자용 목록과 달리 모든 상태(PUBLISHED/HIDDEN/DELETED)를 보고,
 * 모더레이션은 <b>숨김(hide)/복원(restore)</b>만 제공한다(하드 삭제 없음 — dev 숨김 기반 모더레이션과 일치).
 *
 * <ul>
 *   <li>{@link #listForAdmin}: 상태/검색어 필터 목록(미리보기만)</li>
 *   <li>{@link #getForAdmin}: 단건 상세(전체 본문 포함)</li>
 *   <li>{@link #hide}: 공개 → 숨김(HIDDEN). 이미 숨김이면 멱등, 삭제본은 불가</li>
 *   <li>{@link #restore}: 숨김 → 공개(PUBLISHED). 이미 공개면 멱등, 삭제본은 불가</li>
 * </ul>
 */
public interface AdminSharingPostUseCase {

    /** status가 null/blank면 전체 상태, q가 null/blank면 검색어 필터 생략. */
    Page<AdminSharingPostResponse> listForAdmin(String status, String q, Pageable pageable);

    AdminSharingPostResponse getForAdmin(Long postId);

    /** 공개 게시글을 숨김 처리하고 갱신된 상태를 돌려준다. */
    AdminSharingPostResponse hide(Long postId);

    /** 숨긴 게시글을 다시 공개하고 갱신된 상태를 돌려준다. */
    AdminSharingPostResponse restore(Long postId);
}
