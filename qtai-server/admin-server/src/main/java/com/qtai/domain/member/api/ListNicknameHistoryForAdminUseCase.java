package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.NicknameHistoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 관리자 회원 상세 — 닉네임 변경 이력 조회 UseCase 포트. */
public interface ListNicknameHistoryForAdminUseCase {

    /** 회원의 닉네임 변경 이력을 최신순으로 페이징 조회한다. */
    Page<NicknameHistoryItem> listNicknameHistory(Long memberId, Pageable pageable);
}
