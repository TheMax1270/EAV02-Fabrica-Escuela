package com.eav02.backend.messaging.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.messaging.entity.Message;

/**
 * El historial se pagina por keyset sobre (createdAt, id): estable aunque lleguen mensajes nuevos.
 * El Pageable solo aporta el limite; el orden lo fija cada consulta.
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findByIdAndConversation_Id(UUID id, UUID conversationId);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :conversationId "
            + "order by m.createdAt desc, m.id desc")
    List<Message> findLatest(@Param("conversationId") UUID conversationId, Pageable limit);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :conversationId "
            + "and (m.createdAt < :createdAt or (m.createdAt = :createdAt and m.id < :id)) "
            + "order by m.createdAt desc, m.id desc")
    List<Message> findBefore(@Param("conversationId") UUID conversationId, @Param("createdAt") Instant createdAt,
            @Param("id") UUID id, Pageable limit);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :conversationId "
            + "and (m.createdAt > :createdAt or (m.createdAt = :createdAt and m.id > :id)) "
            + "order by m.createdAt asc, m.id asc")
    List<Message> findAfter(@Param("conversationId") UUID conversationId, @Param("createdAt") Instant createdAt,
            @Param("id") UUID id, Pageable limit);

    @Query(value = "SELECT DISTINCT ON (m.conversation_id) m.* FROM messaging.messages m "
            + "WHERE m.conversation_id IN (:conversationIds) "
            + "ORDER BY m.conversation_id, m.created_at DESC, m.id DESC", nativeQuery = true)
    List<Message> findLatestForConversations(@Param("conversationIds") Collection<UUID> conversationIds);
}
