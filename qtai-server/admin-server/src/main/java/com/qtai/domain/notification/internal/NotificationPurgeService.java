package com.qtai.domain.notification.internal;

import com.qtai.domain.notification.api.PurgeMemberNotificationDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * notification 도메인 — 회원 보존기간 만료 정리 구현.
 * 자기 도메인 테이블(notifications)만 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationPurgeService implements PurgeMemberNotificationDataUseCase {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        return jdbc.update("DELETE FROM notifications WHERE member_id = ?", memberId);
    }
}
