package com.eav02.backend.messaging.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.BlockedUserException;
import com.eav02.backend.common.exception.InvalidMessageCursorException;
import com.eav02.backend.common.exception.SelfMessageException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.repository.UserBlockRepository;
import com.eav02.backend.messaging.dto.ConversationPageResponse;
import com.eav02.backend.messaging.dto.ConversationSummaryResponse;
import com.eav02.backend.messaging.dto.MessageRequest;
import com.eav02.backend.messaging.dto.MessageResponse;
import com.eav02.backend.messaging.dto.MessageSliceResponse;
import com.eav02.backend.messaging.dto.ParticipantResponse;
import com.eav02.backend.messaging.entity.Conversation;
import com.eav02.backend.messaging.entity.ConversationPair;
import com.eav02.backend.messaging.entity.Message;
import com.eav02.backend.messaging.repository.ConversationRepository;
import com.eav02.backend.messaging.repository.MessageRepository;
import com.eav02.backend.notification.service.NotificationService;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class MessagingService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    private final UserBlockRepository blocks;
    private final Clock clock;
    private final NotificationService notifications;

    public MessagingService(ConversationRepository conversations, MessageRepository messages, UserRepository users,
            UserBlockRepository blocks, Clock clock, NotificationService notifications) {
        this.conversations = conversations;
        this.messages = messages;
        this.users = users;
        this.blocks = blocks;
        this.clock = clock;
        this.notifications = notifications;
    }

    @Transactional
    public MessageResponse send(UUID senderId, UUID recipientId, MessageRequest request) {
        if (senderId.equals(recipientId)) {
            throw new SelfMessageException();
        }
        if (blocks.existsBetween(senderId, recipientId)) {
            throw new BlockedUserException();
        }
        requireActiveUser(recipientId);
        Instant now = clock.instant();
        ConversationPair pair = ConversationPair.of(senderId, recipientId);
        conversations.insertIfAbsent(UUID.randomUUID(), pair.userOneId(), pair.userTwoId(), now);
        Conversation conversation = conversations.findByUserOne_IdAndUserTwo_Id(pair.userOneId(), pair.userTwoId())
                .orElseThrow(IllegalStateException::new);
        Message message = messages.save(new Message(conversation, users.getReferenceById(senderId),
                request.content(), now));
        conversation.registerMessage(now);
        notifications.newMessage(senderId, recipientId);
        return MessageResponse.from(message, recipientId);
    }

    /**
     * Sin cursor devuelve los mensajes mas recientes; con before, los anteriores a ese mensaje;
     * con after, los posteriores (usado por el polling del cliente). El historial sigue siendo
     * legible con un bloqueo activo: el bloqueo solo impide enviar.
     */
    @Transactional(readOnly = true)
    public MessageSliceResponse history(UUID viewerId, UUID participantId, UUID before, UUID after, int limit) {
        if (viewerId.equals(participantId)) {
            throw new SelfMessageException();
        }
        if (before != null && after != null) {
            throw new InvalidMessageCursorException();
        }
        AppUser participant = users.findById(participantId).orElseThrow(UserNotFoundException::new);
        ParticipantResponse participantView = ParticipantResponse.from(participant);
        ConversationPair pair = ConversationPair.of(viewerId, participantId);
        var conversation = conversations.findByUserOne_IdAndUserTwo_Id(pair.userOneId(), pair.userTwoId());
        if (conversation.isEmpty()) {
            return new MessageSliceResponse(participantView, List.of(), false);
        }

        UUID conversationId = conversation.get().getId();
        Pageable window = PageRequest.of(0, limit + 1);
        List<Message> rows;
        if (after != null) {
            Message cursor = cursor(after, conversationId);
            rows = messages.findAfter(conversationId, cursor.getCreatedAt(), cursor.getId(), window);
        } else if (before != null) {
            Message cursor = cursor(before, conversationId);
            rows = messages.findBefore(conversationId, cursor.getCreatedAt(), cursor.getId(), window);
        } else {
            rows = messages.findLatest(conversationId, window);
        }

        boolean hasMore = rows.size() > limit;
        List<Message> visible = new ArrayList<>(rows.subList(0, Math.min(limit, rows.size())));
        if (after == null) {
            Collections.reverse(visible);
        }
        List<MessageResponse> content = visible.stream()
                .map(message -> MessageResponse.from(message, recipientOf(message, viewerId, participantId)))
                .toList();
        return new MessageSliceResponse(participantView, content, hasMore);
    }

    @Transactional(readOnly = true)
    public ConversationPageResponse inbox(UUID viewerId, int page, int size) {
        Page<Conversation> result = conversations.findForUser(viewerId, PageRequest.of(page, size));
        Map<UUID, Message> latest = result.isEmpty() ? Map.of()
                : messages.findLatestForConversations(result.map(Conversation::getId).getContent()).stream()
                        .collect(Collectors.toMap(message -> message.getConversation().getId(), Function.identity()));
        return ConversationPageResponse.from(result.map(conversation ->
                summary(conversation, viewerId, latest.get(conversation.getId()))));
    }

    private ConversationSummaryResponse summary(Conversation conversation, UUID viewerId, Message lastMessage) {
        AppUser participant = conversation.participantOtherThan(viewerId);
        MessageResponse last = lastMessage == null ? null
                : MessageResponse.from(lastMessage, recipientOf(lastMessage, viewerId, participant.getId()));
        return new ConversationSummaryResponse(conversation.getId(), ParticipantResponse.from(participant), last,
                conversation.getLastMessageAt());
    }

    private Message cursor(UUID messageId, UUID conversationId) {
        return messages.findByIdAndConversation_Id(messageId, conversationId)
                .orElseThrow(InvalidMessageCursorException::new);
    }

    private UUID recipientOf(Message message, UUID viewerId, UUID participantId) {
        return message.getSender().getId().equals(viewerId) ? participantId : viewerId;
    }

    private void requireActiveUser(UUID userId) {
        users.findById(userId).filter(AppUser::isEnabled).orElseThrow(UserNotFoundException::new);
    }
}
