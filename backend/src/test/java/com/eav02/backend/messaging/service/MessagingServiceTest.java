package com.eav02.backend.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.BlockedUserException;
import com.eav02.backend.common.exception.InvalidMessageCursorException;
import com.eav02.backend.common.exception.SelfMessageException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.repository.UserBlockRepository;
import com.eav02.backend.messaging.dto.MessageRequest;
import com.eav02.backend.messaging.entity.Conversation;
import com.eav02.backend.messaging.entity.ConversationPair;
import com.eav02.backend.messaging.entity.Message;
import com.eav02.backend.messaging.repository.ConversationRepository;
import com.eav02.backend.messaging.repository.MessageRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant EARLIER = NOW.minusSeconds(3600);

    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock UserRepository users;
    @Mock UserBlockRepository blocks;

    private MessagingService service;
    private AppUser sender;
    private AppUser recipient;
    private ConversationPair pair;

    @BeforeEach
    void setUp() {
        service = new MessagingService(conversations, messages, users, blocks, Clock.fixed(NOW, ZoneOffset.UTC));
        sender = user(UUID.randomUUID(), "ada", true);
        recipient = user(UUID.randomUUID(), "linus", true);
        pair = ConversationPair.of(sender.getId(), recipient.getId());
    }

    @Test
    void sendsMessageWithSenderRecipientDateAndConversation() {
        Conversation conversation = conversation();
        when(users.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(conversations.findByUserOne_IdAndUserTwo_Id(pair.userOneId(), pair.userTwoId()))
                .thenReturn(Optional.of(conversation));
        when(users.getReferenceById(sender.getId())).thenReturn(sender);
        when(messages.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.send(sender.getId(), recipient.getId(), new MessageRequest("  Hola  "));

        verify(conversations).insertIfAbsent(any(), eq(pair.userOneId()), eq(pair.userTwoId()), eq(NOW));
        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(saved.capture());
        assertThat(saved.getValue().getSender()).isSameAs(sender);
        assertThat(saved.getValue().getConversation()).isSameAs(conversation);
        assertThat(saved.getValue().getContent()).isEqualTo("Hola");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.senderId()).isEqualTo(sender.getId());
        assertThat(response.recipientId()).isEqualTo(recipient.getId());
        assertThat(response.conversationId()).isEqualTo(conversation.getId());
        assertThat(conversation.getLastMessageAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsMessagingSelfWithoutWriting() {
        assertThrows(SelfMessageException.class,
                () -> service.send(sender.getId(), sender.getId(), new MessageRequest("Hola")));
        verifyNoInteractions(conversations, messages, users, blocks);
    }

    @Test
    void rejectsSendingWhenBlockExistsWithoutWriting() {
        when(blocks.existsBetween(sender.getId(), recipient.getId())).thenReturn(true);

        assertThrows(BlockedUserException.class,
                () -> service.send(sender.getId(), recipient.getId(), new MessageRequest("Hola")));
        verifyNoInteractions(conversations, messages);
    }

    @Test
    void rejectsSendingToMissingOrDisabledUser() {
        AppUser disabled = user(recipient.getId(), "linus", false);
        when(users.findById(recipient.getId())).thenReturn(Optional.of(disabled));

        assertThrows(UserNotFoundException.class,
                () -> service.send(sender.getId(), recipient.getId(), new MessageRequest("Hola")));
        verifyNoInteractions(conversations, messages);
    }

    @Test
    void historyWithoutConversationIsEmpty() {
        when(users.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        var slice = service.history(sender.getId(), recipient.getId(), null, null, 30);

        assertThat(slice.participant().username()).isEqualTo("linus");
        assertThat(slice.content()).isEmpty();
        assertThat(slice.hasMore()).isFalse();
    }

    @Test
    void historyReturnsLatestWindowInChronologicalOrder() {
        Conversation conversation = conversation();
        stubConversation(conversation);
        Message newest = message(conversation, recipient, "3", NOW);
        Message middle = message(conversation, sender, "2", NOW.minusSeconds(60));
        Message oldest = message(conversation, recipient, "1", NOW.minusSeconds(120));
        when(messages.findLatest(conversation.getId(), PageRequest.of(0, 3))).thenReturn(List.of(newest, middle, oldest));

        var slice = service.history(sender.getId(), recipient.getId(), null, null, 2);

        assertThat(slice.content()).extracting("content").containsExactly("2", "3");
        assertThat(slice.content().get(0).recipientId()).isEqualTo(recipient.getId());
        assertThat(slice.content().get(1).recipientId()).isEqualTo(sender.getId());
        assertThat(slice.hasMore()).isTrue();
    }

    @Test
    void historyBeforeCursorPagesOlderMessages() {
        Conversation conversation = conversation();
        stubConversation(conversation);
        Message cursor = message(conversation, sender, "cursor", NOW);
        Message older = message(conversation, recipient, "older", EARLIER);
        when(messages.findByIdAndConversation_Id(cursor.getId(), conversation.getId())).thenReturn(Optional.of(cursor));
        when(messages.findBefore(eq(conversation.getId()), eq(NOW), eq(cursor.getId()), any(Pageable.class)))
                .thenReturn(List.of(older));

        var slice = service.history(sender.getId(), recipient.getId(), cursor.getId(), null, 30);

        assertThat(slice.content()).extracting("content").containsExactly("older");
        assertThat(slice.hasMore()).isFalse();
    }

    @Test
    void historyAfterCursorReturnsNewerMessagesAscending() {
        Conversation conversation = conversation();
        stubConversation(conversation);
        Message cursor = message(conversation, sender, "cursor", EARLIER);
        Message first = message(conversation, recipient, "a", NOW.minusSeconds(10));
        Message second = message(conversation, recipient, "b", NOW);
        when(messages.findByIdAndConversation_Id(cursor.getId(), conversation.getId())).thenReturn(Optional.of(cursor));
        when(messages.findAfter(eq(conversation.getId()), eq(EARLIER), eq(cursor.getId()), any(Pageable.class)))
                .thenReturn(List.of(first, second));

        var slice = service.history(sender.getId(), recipient.getId(), null, cursor.getId(), 30);

        assertThat(slice.content()).extracting("content").containsExactly("a", "b");
    }

    @Test
    void historyRejectsCursorFromAnotherConversation() {
        Conversation conversation = conversation();
        stubConversation(conversation);
        UUID foreign = UUID.randomUUID();
        when(messages.findByIdAndConversation_Id(foreign, conversation.getId())).thenReturn(Optional.empty());

        assertThrows(InvalidMessageCursorException.class,
                () -> service.history(sender.getId(), recipient.getId(), foreign, null, 30));
    }

    @Test
    void historyRejectsBothCursors() {
        assertThrows(InvalidMessageCursorException.class,
                () -> service.history(sender.getId(), recipient.getId(), UUID.randomUUID(), UUID.randomUUID(), 30));
        verifyNoInteractions(conversations, messages);
    }

    @Test
    void inboxShowsOtherParticipantAndLastMessage() {
        Conversation conversation = conversation();
        Message last = message(conversation, recipient, "Nos vemos", NOW);
        when(conversations.findForUser(sender.getId(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(conversation), PageRequest.of(0, 20), 1));
        when(messages.findLatestForConversations(List.of(conversation.getId()))).thenReturn(List.of(last));

        var page = service.inbox(sender.getId(), 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        var summary = page.content().get(0);
        assertThat(summary.participant().id()).isEqualTo(recipient.getId());
        assertThat(summary.lastMessage().content()).isEqualTo("Nos vemos");
        assertThat(summary.lastMessage().recipientId()).isEqualTo(sender.getId());
    }

    private void stubConversation(Conversation conversation) {
        when(users.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(conversations.findByUserOne_IdAndUserTwo_Id(pair.userOneId(), pair.userTwoId()))
                .thenReturn(Optional.of(conversation));
    }

    private Conversation conversation() {
        AppUser one = pair.userOneId().equals(sender.getId()) ? sender : recipient;
        AppUser two = one == sender ? recipient : sender;
        Conversation conversation = new Conversation(one, two, EARLIER);
        ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());
        return conversation;
    }

    private Message message(Conversation conversation, AppUser author, String content, Instant at) {
        Message message = new Message(conversation, author, content, at);
        ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
        return message;
    }

    private AppUser user(UUID id, String username, boolean enabled) {
        AppUser user = new AppUser(username, username, username + "@example.com", "hash", UserRole.DEVELOPER,
                enabled, EARLIER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
