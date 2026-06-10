package com.qtai.domain.praise.internal;

import com.qtai.domain.praise.api.PurgeMemberPraiseDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * praise 도메인 — 회원 보존기간 만료 정리 구현.
 * 자기 도메인 테이블(member_praise_songs)만 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class PraisePurgeService implements PurgeMemberPraiseDataUseCase {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        return jdbc.update("DELETE FROM member_praise_songs WHERE member_id = ?", memberId);
    }
}
