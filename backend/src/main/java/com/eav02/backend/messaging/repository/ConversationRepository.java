package com.eav02.backend.messaging.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.messaging.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    // ON CONFLICT evita duplicar la conversacion si ambos usuarios escriben el primer mensaje a la vez.
    @Modifying
    @Query(value = "INSERT INTO messaging.conversations (id, user_one_id, user_two_id, created_at, last_message_at) "
            + "VALUES (:id, :userOneId, :userTwoId, :createdAt, :createdAt) "
            + "ON CONFLICT (user_one_id, user_two_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userOneId") UUID userOneId,
            @Param("userTwoId") UUID userTwoId, @Param("createdAt") Instant createdAt);

    Optional<Conversation> findByUserOne_IdAndUserTwo_Id(UUID userOneId, UUID userTwoId);

    @Query(value = "select c from Conversation c join fetch c.userOne join fetch c.userTwo "
            + "where c.userOne.id = :userId or c.userTwo.id = :userId "
            + "order by c.lastMessageAt desc, c.id desc",
            countQuery = "select count(c) from Conversation c where c.userOne.id = :userId or c.userTwo.id = :userId")
    Page<Conversation> findForUser(@Param("userId") UUID userId, Pageable pageable);
}
