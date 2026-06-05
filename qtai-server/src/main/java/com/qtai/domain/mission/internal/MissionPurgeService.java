package com.qtai.domain.mission.internal;

import com.qtai.domain.mission.api.PurgeMemberMissionDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * mission 도메인 — 회원 보존기간 만료 정리 구현.
 * 자기 도메인 테이블(member_mission_progress)만 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class MissionPurgeService implements PurgeMemberMissionDataUseCase {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        return jdbc.update("DELETE FROM member_mission_progress WHERE member_id = ?", memberId);
    }
}
