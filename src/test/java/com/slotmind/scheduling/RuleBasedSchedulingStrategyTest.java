package com.slotmind.scheduling;

import com.slotmind.domain.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedSchedulingStrategyTest {
    private final RuleBasedSchedulingStrategy strategy = new RuleBasedSchedulingStrategy();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void providerOfflineExcludesOfflineProviderAndSelectsNearestMatchingSpecialty() {
        var original = provider("P1", Specialty.GENERAL, ProviderStatus.OFFLINE);
        var offlineOpen = slot("S1", now.plusMinutes(10), original, SlotStatus.OPEN);
        var compatible = provider("P2", Specialty.GENERAL, ProviderStatus.BUSY);
        var selected = slot("S2", now.plusMinutes(20), compatible, SlotStatus.OPEN);
        var appointment = appointment(original, slot("OLD", now, original, SlotStatus.BOOKED));

        var result = strategy.recommend(new SchedulingContext(appointment, List.of(original, compatible),
                List.of(offlineOpen, selected), TriggerReason.PROVIDER_OFFLINE));

        assertThat(result).extracting(recommendation -> recommendation.slot().getId()).containsExactly("S2");
    }

    @Test
    void slotCancelledPrefersOriginalProviderBeforeCompatibleProvider() {
        var original = provider("P1", Specialty.GENERAL, ProviderStatus.BUSY);
        var sameProvider = slot("S1", now.plusMinutes(30), original, SlotStatus.OPEN);
        var compatible = provider("P2", Specialty.GENERAL, ProviderStatus.AVAILABLE);
        var other = slot("S2", now.plusMinutes(10), compatible, SlotStatus.OPEN);
        var appointment = appointment(original, slot("OLD", now, original, SlotStatus.CANCELLED));

        var result = strategy.recommend(new SchedulingContext(appointment, List.of(original, compatible),
                List.of(other, sameProvider), TriggerReason.SLOT_CANCELLED));

        assertThat(result).extracting(recommendation -> recommendation.slot().getId()).containsExactly("S1");
    }

    @Test
    void neverRecommendsNonOpenSlotsOrOfflineProviders() {
        var original = provider("P1", Specialty.GENERAL, ProviderStatus.AVAILABLE);
        var offline = provider("P2", Specialty.GENERAL, ProviderStatus.OFFLINE);
        var appointment = appointment(original, slot("OLD", now, original, SlotStatus.BOOKED));

        var result = strategy.recommend(new SchedulingContext(appointment, List.of(original, offline), List.of(
                slot("BOOKED", now.plusMinutes(10), original, SlotStatus.BOOKED),
                slot("OFFLINE", now.plusMinutes(20), offline, SlotStatus.OPEN)), TriggerReason.INITIAL));

        assertThat(result).isEmpty();
    }

    private Provider provider(String id, Specialty specialty, ProviderStatus status) {
        return new Provider(id, id, specialty, status);
    }
    private Slot slot(String id, LocalDateTime start, Provider provider, SlotStatus status) {
        return new Slot(id, start, 30, provider, status);
    }
    private Appointment appointment(Provider provider, Slot slot) {
        return new Appointment("A1", "Patient", AppointmentType.ROUTINE, provider, slot, AppointmentStatus.CONFIRMED);
    }
}