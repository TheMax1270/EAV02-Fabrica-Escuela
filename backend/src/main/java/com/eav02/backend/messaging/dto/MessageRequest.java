package com.eav02.backend.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(@NotBlank @Size(max = 2000) String content) {
    public MessageRequest { content = content == null ? null : content.trim(); }
}
