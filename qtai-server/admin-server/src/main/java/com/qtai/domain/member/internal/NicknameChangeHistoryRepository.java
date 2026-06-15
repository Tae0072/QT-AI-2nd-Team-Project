package com.qtai.domain.member.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 닉네임 변경 이력 조회(관리자). */
public interface NicknameChangeHistoryRepository extends JpaRepository<NicknameChangeHistory, Long> {

    /** 회원의 닉네임 변경 이력을 최신순으로 페이징 조회한다. */
    Page<NicknameChangeHistory> findByMemberIdOrderByChangedAtDesc(Long memberId, Pageable pageable);
}
