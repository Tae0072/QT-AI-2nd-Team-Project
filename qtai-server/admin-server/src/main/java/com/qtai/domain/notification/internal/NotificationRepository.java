package com.qtai.domain.notification.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 알림 영속성 포트. Spring Data JPA로 구현.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 특정 회원의 전체 알림 (최신순). */
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /** 특정 회원의 미읽음 알림 (최신순). */
    Page<Notification> findByMemberIdAndReadAtIsNullOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /** 미읽음 알림 수. */
    long countByMemberIdAndReadAtIsNull(Long memberId);

    /** 동일 이벤트 알림 중복 확인 (멱등성 보장). */
    boolean existsByMemberIdAndEventKey(Long memberId, String eventKey);

    @Query("SELECT n.eventKey FROM Notification n WHERE n.eventKey IN :eventKeys")
    List<String> findEventKeysIn(@Param("eventKeys") Collection<String> eventKeys);

    /** 전체 일괄 읽음 처리 — 영향받은 행 수 반환. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
            "WHERE n.memberId = :memberId AND n.readAt IS NULL")
    int markAllAsRead(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);
}
