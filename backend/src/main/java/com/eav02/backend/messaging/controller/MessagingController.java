package com.eav02.backend.messaging.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.messaging.dto.ConversationPageResponse;
import com.eav02.backend.messaging.dto.MessageRequest;
import com.eav02.backend.messaging.dto.MessageResponse;
import com.eav02.backend.messaging.dto.MessageSliceResponse;
import com.eav02.backend.messaging.service.MessagingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/conversations")
public class MessagingController {

    private final MessagingService messaging;

    public MessagingController(MessagingService messaging) {
        this.messaging = messaging;
    }

    @GetMapping
    public ResponseEntity<ConversationPageResponse> inbox(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(messaging.inbox(viewer(jwt), page, size));
    }

    @GetMapping("/{participantId}/messages")
    public ResponseEntity<MessageSliceResponse> history(@PathVariable UUID participantId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID before,
            @RequestParam(required = false) UUID after,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(messaging.history(viewer(jwt), participantId, before, after, limit));
    }

    @PostMapping("/{participantId}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable UUID participantId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messaging.send(viewer(jwt), participantId, request));
    }

    private UUID viewer(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
