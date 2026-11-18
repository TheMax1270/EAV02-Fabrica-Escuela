package com.eav02.backend.messaging.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationPairTest {

    @Test
    void ordersPairLikePostgresRegardlessOfArgumentOrder() {
        UUID low = UUID.fromString("10000000-0000-0000-0000-000000000000");
        UUID high = UUID.fromString("80000000-0000-0000-0000-000000000000");
        // UUID.compareTo usa longs con signo y consideraria 8000... menor que 1000...
        assertThat(high.compareTo(low)).isNegative();

        assertThat(ConversationPair.of(high, low)).isEqualTo(new ConversationPair(low, high));
        assertThat(ConversationPair.of(low, high)).isEqualTo(new ConversationPair(low, high));
    }
}
