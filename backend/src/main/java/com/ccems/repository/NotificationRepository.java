package com.ccems.repository;

import com.ccems.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<Notification> findByNotificationIdAndUserId(Long notificationId, Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :uid and n.readAt is null")
    int markAllRead(@Param("uid") Long uid, @Param("now") LocalDateTime now);
}
