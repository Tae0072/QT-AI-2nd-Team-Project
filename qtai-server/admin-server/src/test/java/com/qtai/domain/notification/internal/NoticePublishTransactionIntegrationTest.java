package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.domain.member.api.ListActiveMemberIdsUseCase;
import com.qtai.domain.notification.api.dto.AdminNoticeCommand;
import com.qtai.domain.notification.api.dto.AdminNoticePublishResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NoticePublishTransactionIntegrationTest {

    private static final long MEMBER_ID = 90_001L;
    private static final long ADMIN_USER_ID = 91_001L;

    @Autowired
    NoticeService noticeService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    StubActiveMemberIdsUseCase activeMemberIdsUseCase;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM notifications WHERE member_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE admin_user_id = ?", ADMIN_USER_ID);
        jdbcTemplate.update("DELETE FROM notices WHERE admin_user_id = ?", ADMIN_USER_ID);
        jdbcTemplate.update("DELETE FROM admin_users WHERE id = ?", ADMIN_USER_ID);
        jdbcTemplate.update("DELETE FROM members WHERE id = ?", MEMBER_ID);
        jdbcTemplate.update("""
                INSERT INTO members
                    (id, kakao_id, email, nickname, status, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, MEMBER_ID, MEMBER_ID, "notice-tx@example.com", "notice_tx_user");
        jdbcTemplate.update("""
                INSERT INTO admin_users
                    (id, member_id, admin_role, status, created_at, updated_at)
                VALUES (?, ?, 'OPERATOR', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ADMIN_USER_ID, MEMBER_ID);
    }

    @Test
    void publish_keepsNoticePublishedWhenNotificationChunkRollsBack() {
        activeMemberIdsUseCase.setMemberIds(Arrays.asList(MEMBER_ID, null));
        var created = noticeService.createNotice(new AdminNoticeCommand(
                ADMIN_USER_ID, "공지", "본문", null));

        AdminNoticePublishResponse response = noticeService.publishNotice(ADMIN_USER_ID, created.id());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM notices WHERE id = ?", String.class, created.id());
        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE notice_id = ?", Integer.class, created.id());
        assertThat(status).isEqualTo("PUBLISHED");
        assertThat(response.notificationResult().requestedCount()).isEqualTo(2);
        assertThat(response.notificationResult().createdCount()).isEqualTo(1);
        assertThat(response.notificationResult().failedCount()).isEqualTo(1);
        assertThat(notificationCount).isEqualTo(1);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        StubActiveMemberIdsUseCase stubActiveMemberIdsUseCase() {
            return new StubActiveMemberIdsUseCase();
        }
    }

    static class StubActiveMemberIdsUseCase implements ListActiveMemberIdsUseCase {

        private List<Long> memberIds = new ArrayList<>();

        void setMemberIds(List<Long> memberIds) {
            this.memberIds = new ArrayList<>(memberIds);
        }

        @Override
        public List<Long> listActiveMemberIds() {
            return new ArrayList<>(memberIds);
        }
    }
}
