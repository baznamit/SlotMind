package com.slotmind.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotmind.domain.*;
import com.slotmind.scheduling.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class LlmSchedulingStrategyTest {
    private FakeGateway gateway;
    private LlmSchedulingStrategy strategy;
    private Provider original;
    private Provider compatible;
    private Slot oldSlot;
    private Slot openSlot;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        gateway = new FakeGateway();
        var mapper = new ObjectMapper();
        var fallback = new RuleBasedSchedulingStrategy();
        strategy = new LlmSchedulingStrategy(gateway, fallback, mapper,
                new LlmProperties("http://localhost", "test-model", "PC1", "unused", Duration.ofSeconds(1)));
        original = new Provider("P1", "Original", Specialty.GENERAL, ProviderStatus.AVAILABLE);
        compatible = new Provider("P2", "Compatible", Specialty.GENERAL, ProviderStatus.BUSY);
        oldSlot = new Slot("OLD", LocalDateTime.now(), 30, original, SlotStatus.BOOKED);
        openSlot = new Slot("OPEN", LocalDateTime.now().plusHours(1), 30, compatible, SlotStatus.OPEN);
        appointment = new Appointment("A1", "Patient", AppointmentType.ROUTINE, original, oldSlot, AppointmentStatus.CONFIRMED);
    }

    @Test
    void acceptsValidChatCompletionContent() {
        gateway.response = "{\"slotId\":\"OPEN\",\"alternateProviderId\":null,\"confidence\":0.9,\"reasoning\":\"Best available match\"}";

        var result = strategy.recommend(context(TriggerReason.INITIAL));

        assertThat(result).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.slot()).isSameAs(openSlot);
            assertThat(recommendation.confidence()).isEqualTo(0.9);
        });
    }

    @Test
    void malformedJsonFallsBackToRuleBasedStrategy() {
        gateway.response = "not-json";

        var result = strategy.recommend(context(TriggerReason.INITIAL));

        assertThat(result).singleElement().extracting(recommendation -> recommendation.slot()).isEqualTo(openSlot);
    }

    @Test
    void gatewayFailureFallsBack() {
        gateway.failure = new LlmGatewayException("timeout");

        assertThat(strategy.recommend(context(TriggerReason.INITIAL))).hasSize(1);
    }

    @Test
    void invalidOrUnavailableSlotFallsBack() {
        gateway.response = "{\"slotId\":\"UNKNOWN\",\"confidence\":0.8,\"reasoning\":\"text\"}";

        var result = strategy.recommend(context(TriggerReason.INITIAL));

        assertThat(result).singleElement().extracting(recommendation -> recommendation.slot()).isEqualTo(openSlot);
    }

    @Test
    void providerOfflineRecommendationIsRejectedAndFallbackIsUsed() {
        original.setStatus(ProviderStatus.OFFLINE);
        gateway.response = "{\"slotId\":\"OPEN\",\"confidence\":0.9,\"reasoning\":\"bad\"}";

        var result = strategy.recommend(context(TriggerReason.PROVIDER_OFFLINE));

        assertThat(result).singleElement().extracting(recommendation -> recommendation.slot()).isEqualTo(openSlot);
    }

    private SchedulingContext context(TriggerReason reason) {
        return new SchedulingContext(appointment, List.of(original, compatible), List.of(oldSlot, openSlot), reason);
    }

    private static final class FakeGateway implements LlmGateway {
        private String response = "{}";
        private RuntimeException failure;
        @Override
        public String complete(String requestJson) {
            if (failure != null) throw failure;
            return response;
        }
    }
}