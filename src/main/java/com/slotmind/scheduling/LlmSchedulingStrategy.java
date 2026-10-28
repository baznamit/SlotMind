package com.slotmind.scheduling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotmind.ai.LlmGateway;
import com.slotmind.ai.LlmProperties;
import com.slotmind.domain.Provider;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.domain.Slot;
import com.slotmind.domain.SlotStatus;
import com.slotmind.domain.TriggerReason;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("llmSchedulingStrategy")
public class LlmSchedulingStrategy implements SchedulingStrategy {
    private final LlmGateway gateway;
    private final RuleBasedSchedulingStrategy fallback;
    private final ObjectMapper mapper;
    private final LlmProperties properties;

    public LlmSchedulingStrategy(LlmGateway gateway, RuleBasedSchedulingStrategy fallback, ObjectMapper mapper,
                                 LlmProperties properties) {
        this.gateway = gateway; this.fallback = fallback; this.mapper = mapper; this.properties = properties;
    }

    @Override
    public List<SchedulingRecommendation> recommend(SchedulingContext context) {
        try {
            var content = gateway.complete(prompt(context));
            var node = mapper.readTree(content);
            var recommendation = validate(node, context);
            return List.of(recommendation);
        } catch (Exception exception) {
            return fallback.recommend(context);
        }
    }

    private String prompt(SchedulingContext context) {
        var triggerInstruction = switch (context.triggerReason()) {
            case PROVIDER_OFFLINE -> "The original provider is offline. Never select that provider; choose a compatible non-offline provider.";
            case SLOT_CANCELLED -> "The original slot was cancelled. Prefer the original provider; use a compatible provider only if needed.";
            case INITIAL -> "This is a manual suggestion. Prefer the original provider, then a compatible provider.";
        };
        try {
            var request = mapper.createObjectNode();
            request.put("model", properties.model());
            var message = request.putArray("messages").addObject();
            message.put("role", "user");
            message.put("content", triggerInstruction + " Return JSON only with slotId, alternateProviderId, confidence, reasoning. "
                    + "Appointment=" + context.appointment().getId() + ", type=" + context.appointment().getAppointmentType()
                    + ", patient=" + context.appointment().getPatientName()
                    + ", providers=" + context.eligibleProviders().stream().map(this::providerText).toList()
                    + ", slots=" + context.availableSlots().stream().map(this::slotText).toList());
            return mapper.writeValueAsString(request);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create LLM request", exception);
        }
    }

    private SchedulingRecommendation validate(JsonNode node, SchedulingContext context) {
        if (!node.path("slotId").isTextual() || !node.has("confidence") || !node.path("reasoning").isTextual())
            throw new IllegalArgumentException("LLM output is missing required fields");
        double confidence = node.get("confidence").asDouble(Double.NaN);
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1 || node.get("reasoning").asText().isBlank())
            throw new IllegalArgumentException("LLM output has invalid confidence or reasoning");
        Map<String, Provider> providers = context.eligibleProviders().stream().collect(Collectors.toMap(Provider::getId, p -> p, (a, b) -> a));
        Slot slot = context.availableSlots().stream().filter(candidate -> candidate.getId().equals(node.get("slotId").asText())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("LLM recommended an unknown slot"));
        Provider provider = providers.get(slot.getProvider().getId());
        if (slot.getStatus() != SlotStatus.OPEN || provider == null || provider.getStatus() == ProviderStatus.OFFLINE
                || slot.getProvider().getStatus() == ProviderStatus.OFFLINE)
            throw new IllegalArgumentException("LLM recommended an unavailable provider or slot");
        Provider alternate = null;
        if (node.hasNonNull("alternateProviderId") && !node.get("alternateProviderId").asText().isBlank()) {
            alternate = providers.get(node.get("alternateProviderId").asText());
            if (alternate == null || alternate.getStatus() == ProviderStatus.OFFLINE) throw new IllegalArgumentException("Invalid alternate provider");
        }
        var original = context.appointment().getProvider();
        if (context.triggerReason() == TriggerReason.PROVIDER_OFFLINE && provider.getId().equals(original.getId()))
            throw new IllegalArgumentException("Offline provider was recommended");
        if (context.triggerReason() == TriggerReason.SLOT_CANCELLED && provider.getId().equals(original.getId()) == false
                && context.availableSlots().stream().anyMatch(candidate -> candidate.getStatus() == SlotStatus.OPEN
                && candidate.getProvider().getId().equals(original.getId())))
            throw new IllegalArgumentException("Original provider was available for a cancelled slot");
        if (provider.getSpecialty() != original.getSpecialty()) throw new IllegalArgumentException("Provider specialty is incompatible");
        return new SchedulingRecommendation(slot, provider, confidence, node.get("reasoning").asText());
    }

    private String providerText(Provider provider) { return provider.getId() + ":" + provider.getSpecialty() + ":" + provider.getStatus(); }
    private String slotText(Slot slot) { return slot.getId() + ":" + slot.getProvider().getId() + ":" + slot.getStartTime() + ":" + slot.getStatus(); }
}