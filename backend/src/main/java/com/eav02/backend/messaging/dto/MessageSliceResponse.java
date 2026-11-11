package com.eav02.backend.messaging.dto;

import java.util.List;

/** Ventana del historial en orden cronologico ascendente; hasMore indica si quedan mensajes en la direccion pedida. */
public record MessageSliceResponse(ParticipantResponse participant, List<MessageResponse> content, boolean hasMore) {
}
