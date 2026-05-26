package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

/**
 * NotificationRepository 통합 테스트.
 *
 * <p>H2 create-drop 으로 실제 DB 대상 JPQL·derived query·UK 검증.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 12, 0);

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    // ── markAllAsRead ──

    @Test
    @DisplayName("markAllAsRead — 미읽음 알림만 readAt 갱신, 이미 읽은 알림 무영향")
    void markAllAsRead_updates_only_unread() {
        Notification unread1 = createAndSave(1L, "LIKE", "알림1", null, "KEY_1");
        Notification unread2 = createAndSave(1L, "COMMENT", "알림2", null, "KEY_2");
        Notification alreadyRead = createAndSave(1L, "LIKE", "알림3", null, "KEY_3");
        alreadyRead = notificationRepository.findById(alreadyRead.getId()).orElseThrow();
        // 리플렉션으로 readAt 설정 (이미 읽은 상태)
        setReadAt(alreadyRead, NOW.minusDays(1));
        em.persistAndFlush(alreadyRead);

        // 다른 회원 알림 — 영향받지 않아야 함
        createAndSave(2L, "LIKE", "타인알림", null, "KEY_OTHER");

        LocalDateTime readAt = NOW;
        int updated = notificationRepository.markAllAsRead(1L, readAt);

        assertThat(updated).isEqualTo(2);

        // 검증: 미읽음이었던 알림이 readAt 으로 갱신
        em.clear();
        Notification refreshed1 = notificationRepository.findById(unread1.getId()).orElseThrow();
        Notification refreshed2 = notificationRepository.findById(unread2.getId()).orElseThrow();
        assertThat(refreshed1.getReadAt()).isEqualTo(readAt);
        assertThat(refreshed2.getReadAt()).isEqualTo(readAt);

        // 이미 읽은 알림은 변경 없음
        Notification refreshedRead = notificationRepository.findById(alreadyRead.getId()).orElseThrow();
        assertThat(refreshedRead.getReadAt()).isEqualTo(NOW.minusDays(1));

        // 다른 회원 알림은 미변경
        List<Notification> otherMember = notificationRepository
                .findByMemberIdOrderByCreatedAtDesc(2L, PageRequest.of(0, 10)).getContent();
        assertThat(otherMember).hasSize(1);
        assertThat(otherMember.get(0).getReadAt()).isNull();
    }

    @Test
    @DisplayName("markAllAsRead — 미읽음 알림 없으면 0 반환")
    void markAllAsRead_returns_zero_when_no_unread() {
        int updated = notificationRepository.markAllAsRead(999L, NOW);
        assertThat(updated).isZero();
    }

    // ── derived queries ──

    @Test
    @DisplayName("findByMemberIdAndReadAtIsNullOrderByCreatedAtDesc — 미읽음만 최신순")
    void findUnread_ordered_by_createdAt_desc() {
        Notification old = createNotification(1L, "LIKE", "오래된", null, "K1");
        old = saveWithCreatedAt(old, NOW.minusHours(2));

        Notification recent = createNotification(1L, "COMMENT", "최신", null, "K2");
        recent = saveWithCreatedAt(recent, NOW);

        Notification read = createNotification(1L, "LIKE", "읽음", null, "K3");
        read = saveWithCreatedAt(read, NOW.minusHours(1));
        setReadAt(read, NOW);
        em.persistAndFlush(read);

        em.clear();

        Page<Notification> page = notificationRepository
                .findByMemberIdAndReadAtIsNullOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("최신");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("오래된");
    }

    @Test
    @DisplayName("findByMemberIdOrderByCreatedAtDesc — 전체 알림 최신순")
    void findAll_ordered_by_createdAt_desc() {
        saveWithCreatedAt(createNotification(1L, "LIKE", "A", null, "KA"), NOW.minusHours(2));
        saveWithCreatedAt(createNotification(1L, "COMMENT", "B", null, "KB"), NOW);

        Page<Notification> page = notificationRepository
                .findByMemberIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("B");
    }

    @Test
    @DisplayName("countByMemberIdAndReadAtIsNull — 미읽음 수 정확")
    void countUnread() {
        createAndSave(1L, "LIKE", "미읽음1", null, "U1");
        createAndSave(1L, "LIKE", "미읽음2", null, "U2");
        Notification read = createAndSave(1L, "LIKE", "읽음", null, "U3");
        setReadAt(read, NOW);
        em.persistAndFlush(read);

        long count = notificationRepository.countByMemberIdAndReadAtIsNull(1L);
        assertThat(count).isEqualTo(2);
    }

    // ── existsByMemberIdAndEventKey ──

    @Test
    @DisplayName("existsByMemberIdAndEventKey — 존재하면 true")
    void existsByMemberIdAndEventKey_true() {
        createAndSave(1L, "LIKE", "알림", null, "LIKE_1_10");

        assertThat(notificationRepository.existsByMemberIdAndEventKey(1L, "LIKE_1_10")).isTrue();
        assertThat(notificationRepository.existsByMemberIdAndEventKey(1L, "OTHER")).isFalse();
        assertThat(notificationRepository.existsByMemberIdAndEventKey(2L, "LIKE_1_10")).isFalse();
    }

    // ── UK: (member_id, event_key) ──

    @Test
    @DisplayName("UK (member_id, event_key) — 동일 조합 중복 삽입 시 예외")
    void unique_constraint_member_eventKey() {
        createAndSave(1L, "LIKE", "첫번째", null, "DUP_KEY");

        Notification dup = Notification.builder()
                .memberId(1L)
                .type(NotificationType.COMMENT)
                .title("두번째")
                .eventKey("DUP_KEY")
                .createdAt(NOW)
                .build();

        assertThrows(Exception.class, () -> em.persistAndFlush(dup));
    }

    @Test
    @DisplayName("UK — eventKey null 은 중복 허용 (NULL != NULL)")
    void unique_constraint_null_eventKey_allowed() {
        createAndSave(1L, "LIKE", "null key 1", null, null);
        Notification second = Notification.builder()
                .memberId(1L)
                .type(NotificationType.LIKE)
                .title("null key 2")
                .createdAt(NOW)
                .build();
        em.persistAndFlush(second);

        long count = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10)).getTotalElements();
        assertThat(count).isEqualTo(2);
    }

    // ── helpers ──

    private Notification createNotification(Long memberId, String type, String title,
                                             String body, String eventKey) {
        return Notification.builder()
                .memberId(memberId)
                .type(NotificationType.valueOf(type))
                .title(title)
                .body(body)
                .eventKey(eventKey)
                .createdAt(NOW)
                .build();
    }

    private Notification createAndSave(Long memberId, String type, String title,
                                        String body, String eventKey) {
        return em.persistAndFlush(createNotification(memberId, type, title, body, eventKey));
    }

    private Notification saveWithCreatedAt(Notification n, LocalDateTime createdAt) {
        try {
            var field = Notification.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(n, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return em.persistAndFlush(n);
    }

    private void setReadAt(Notification n, LocalDateTime readAt) {
        try {
            var field = Notification.class.getDeclaredField("readAt");
            field.setAccessible(true);
            field.set(n, readAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
