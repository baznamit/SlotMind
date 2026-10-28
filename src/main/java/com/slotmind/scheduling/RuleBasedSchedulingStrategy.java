package com.slotmind.scheduling;

import com.slotmind.domain.Provider;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.domain.Slot;
import com.slotmind.domain.SlotStatus;
import com.slotmind.domain.TriggerReason;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RuleBasedSchedulingStrategy implements SchedulingStrategy {
    @Override
    public List<SchedulingRecommendation> recommend(SchedulingContext context) {
        var appointment = context.appointment();
        var originalProvider = appointment.getProvider();
        var providers = context.eligibleProviders().stream()
                .filter(provider -> provider.getStatus() != ProviderStatus.OFFLINE)
                .collect(Collectors.toMap(Provider::getId, provider -> provider, (first, ignored) -> first));
        var candidates = context.availableSlots().stream()
                .filter(slot -> slot.getStatus() == SlotStatus.OPEN)
                .filter(slot -> providers.containsKey(slot.getProvider().getId()))
                .filter(slot -> slot.getProvider().getStatus() != ProviderStatus.OFFLINE)
                .filter(slot -> !slot.getProvider().getId().equals(originalProvider.getId())
                        || context.triggerReason() != TriggerReason.PROVIDER_OFFLINE)
                .filter(slot -> slot.getId() == null || !slot.getId().equals(appointment.getSlot().getId()))
                .sorted(Comparator.comparing(Slot::getStartTime))
                .toList();

        if (context.triggerReason() == TriggerReason.SLOT_CANCELLED
                || context.triggerReason() == TriggerReason.INITIAL) {
            var sameProvider = candidates.stream()
                    .filter(slot -> slot.getProvider().getId().equals(originalProvider.getId()))
                    .toList();
            if (!sameProvider.isEmpty()) return recommendations(sameProvider, providers, "Nearest open slot with the original provider.");
        }
        var compatible = candidates.stream()
                .filter(slot -> slot.getProvider().getSpecialty() == originalProvider.getSpecialty())
                .toList();
        return recommendations(compatible, providers, context.triggerReason() == TriggerReason.PROVIDER_OFFLINE
                ? "Nearest open slot with a compatible non-offline provider."
                : "Nearest open slot with a compatible provider.");
    }

    private List<SchedulingRecommendation> recommendations(List<Slot> slots, java.util.Map<String, Provider> providers,
                                                           String reasoning) {
        return slots.stream().map(slot -> new SchedulingRecommendation(slot, providers.get(slot.getProvider().getId()), 1.0, reasoning)).toList();
    }
}