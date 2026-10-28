package com.slotmind.api;

import com.slotmind.domain.SuggestionStatus;
import com.slotmind.domain.TriggerReason;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class SuggestionDtos {
    private SuggestionDtos() { }
    public record DecisionRequest(@NotNull SuggestionStatus decision) { }
    public record SuggestionResponse(String id, String appointmentId, String recommendedSlotId,
                                     String providerId, Double confidence, String reasoning,
                                     SuggestionStatus status, TriggerReason triggerReason,
                                     LocalDateTime recommendedStartTime) { }
}