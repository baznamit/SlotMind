package com.slotmind.api;

import com.slotmind.service.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void unavailableSlotIsReportedAsSafeConflict() {
        var response = handler.conflict(new ConflictException("Recommended slot is no longer available"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo(
                "This slot has already been booked. Refresh the dashboard and reject this outdated suggestion.");
        assertThat(response.getBody().message()).doesNotContain("Recommended slot");
    }

    @Test
    void staleSuggestionIsReportedAsConflict() {
        var response = handler.conflict(new ConflictException("Suggestion is no longer pending"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo(
                "This suggestion is no longer pending. Refresh the dashboard.");
    }
}