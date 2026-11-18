package com.eav02.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationExceptionHandlerTest {

    @Test
    void missingNotificationIsReportedAs404WithAStableCode() {
        var response = new GlobalExceptionHandler().notificationNotFound(new NotificationNotFoundException());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(response.getBody().errors()).isEmpty();
    }
}