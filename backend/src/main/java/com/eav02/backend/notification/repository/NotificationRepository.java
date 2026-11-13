package com.eav02.backend.notification.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ON CONFLICT DO NOTHING apoya el indice unico parcial: evita duplicados de "me gusta" y seguidor sin leer.
    @Modifying
    @Query(value = "INSERT INTO notifications.notifications "
            + "(id, recipient_id, actor_id, type, target_type, target_id, target_label, created_at) "
            + "VALUES (gen_random_uuid(), :recipientId, :actorId, :type, :targetType, :targetId, :targetLabel, :createdAt) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insert(@Param("recipientId") UUID recipientId, @Param("actorId") UUID actorId,
            @Param("type") String type, @Param("targetType") String targetType,
            @Param("targetId") UUID targetId, @Param("targetLabel") String targetLabel,
            @Param("createdAt") Instant createdAt);

    @Query(value = "select n from Notification n join fetch n.actor where n.recipient.id = :userId",
            countQuery = "select count(n) from Notification n where n.recipient.id = :userId")
    Page<Notification> findForRecipient(@Param("userId") UUID userId, Pageable pageable);

    long countByRecipient_IdAndReadAtIsNull(UUID userId);

    @Query("select n from Notification n join fetch n.actor where n.id = :id and n.recipient.id = :userId")
    Optional<Notification> findOwnedById(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}