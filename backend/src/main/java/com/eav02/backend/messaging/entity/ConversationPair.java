package com.eav02.backend.messaging.entity;

import java.util.UUID;

/**
 * Par de participantes normalizado tal como lo exige ck_conversations_ordered_pair.
 * Se compara el texto del UUID y no UUID.compareTo: Java compara longs con signo,
 * mientras PostgreSQL ordena uuid byte a byte (equivalente al orden del hex en minusculas).
 */
public record ConversationPair(UUID userOneId, UUID userTwoId) {

    public static ConversationPair of(UUID first, UUID second) {
        return first.toString().compareTo(second.toString()) < 0
                ? new ConversationPair(first, second)
                : new ConversationPair(second, first);
    }
}
