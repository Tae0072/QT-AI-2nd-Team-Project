package com.qtai.domain.notification.internal;

/**
 * 알림 영속성 포트. Spring Data JPA로 구현.
 */
public interface NotificationRepository {

    // TODO: extends JpaRepository<Notification, Long>
    // TODO: Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    // TODO: Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    // TODO: @Modifying @Query("UPDATE Notification n SET n.read=true WHERE n.recipientId=:id AND n.read=false") int markAllAsRead(...);
}
