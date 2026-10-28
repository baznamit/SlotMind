package com.slotmind.service;

import com.slotmind.domain.*;
import com.slotmind.repository.AppointmentRepository;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.repository.RescheduleSuggestionRepository;
import com.slotmind.repository.SlotRepository;
import com.slotmind.scheduling.RuleBasedSchedulingStrategy;
import com.slotmind.scheduling.SchedulingStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock ProviderRepository providers;
    @Mock SlotRepository slots;
    @Mock RescheduleSuggestionRepository suggestions;
    @Mock SchedulingStrategyRegistry strategyRegistry;
    private SuggestionService service;
    private Provider provider;
    private Slot oldSlot;
    private Slot newSlot;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new SuggestionService(appointments, providers, slots, suggestions, strategyRegistry);
        provider = new Provider("P1", "Provider", Specialty.GENERAL, ProviderStatus.AVAILABLE);
        oldSlot = new Slot("OLD", LocalDateTime.now(), 30, provider, SlotStatus.BOOKED);
        newSlot = new Slot("NEW", LocalDateTime.now().plusHours(1), 30, provider, SlotStatus.OPEN);
        appointment = new Appointment("A1", "Patient", AppointmentType.ROUTINE, provider, oldSlot, AppointmentStatus.CONFIRMED);
    }

    @Test
    void createsPendingSuggestionWithoutChangingSlot() {
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));
        when(suggestions.findByAppointment_IdAndTriggerReasonAndStatus("A1", TriggerReason.INITIAL,
                SuggestionStatus.PENDING)).thenReturn(Optional.empty());
        when(providers.findAll()).thenReturn(List.of(provider));
        when(slots.findAll()).thenReturn(List.of(oldSlot, newSlot));
        when(strategyRegistry.selected()).thenReturn(new RuleBasedSchedulingStrategy());
        when(suggestions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.generate("A1", TriggerReason.INITIAL);

        assertThat(response.status()).isEqualTo(SuggestionStatus.PENDING);
        assertThat(response.recommendedSlotId()).isEqualTo("NEW");
        assertThat(newSlot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    void reportsNoEligibleSlot() {
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));
        when(suggestions.findByAppointment_IdAndTriggerReasonAndStatus("A1", TriggerReason.INITIAL,
                SuggestionStatus.PENDING)).thenReturn(Optional.empty());
        when(providers.findAll()).thenReturn(List.of(provider));
        when(slots.findAll()).thenReturn(List.of(oldSlot));
        when(strategyRegistry.selected()).thenReturn(new RuleBasedSchedulingStrategy());

        assertThatThrownBy(() -> service.generate("A1", TriggerReason.INITIAL))
                .isInstanceOf(NoEligibleSlotException.class);
        verify(suggestions, never()).save(any());
    }

    @Test
    void repeatedTriggerReturnsExistingPendingSuggestion() {
        var existing = suggestion();
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));
        when(suggestions.findByAppointment_IdAndTriggerReasonAndStatus("A1", TriggerReason.PROVIDER_OFFLINE,
                SuggestionStatus.PENDING)).thenReturn(Optional.of(existing));

        var response = service.generate("A1", TriggerReason.PROVIDER_OFFLINE);

        assertThat(response.id()).isEqualTo("SUG");
        verify(strategyRegistry, never()).selected();
        verify(suggestions, never()).save(any());
    }

    @Test
    void rejectsWithoutChangingBooking() {
        var suggestion = suggestion();
        when(suggestions.findById("SUG")).thenReturn(Optional.of(suggestion));

        var response = service.decide("SUG", SuggestionStatus.REJECTED);

        assertThat(response.status()).isEqualTo(SuggestionStatus.REJECTED);
        assertThat(oldSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(newSlot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    void acceptsAndMovesBookingAtomically() {
        var suggestion = suggestion();
        when(suggestions.findById("SUG")).thenReturn(Optional.of(suggestion));
        when(slots.findById("NEW")).thenReturn(Optional.of(newSlot));
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));

        var response = service.decide("SUG", SuggestionStatus.ACCEPTED);

        assertThat(response.status()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(appointment.getSlot()).isSameAs(newSlot);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(oldSlot.getStatus()).isEqualTo(SlotStatus.OPEN);
        assertThat(newSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void staleSuggestionCannotBeAcceptedTwice() {
        var suggestion = suggestion();
        when(suggestions.findById("SUG")).thenReturn(Optional.of(suggestion));
        when(slots.findById("NEW")).thenReturn(Optional.of(newSlot));
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));
        service.decide("SUG", SuggestionStatus.ACCEPTED);

        assertThatThrownBy(() -> service.decide("SUG", SuggestionStatus.ACCEPTED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unavailableTargetSlotDoesNotChangeBooking() {
        var suggestion = suggestion();
        newSlot.setStatus(SlotStatus.BOOKED);
        when(suggestions.findById("SUG")).thenReturn(Optional.of(suggestion));
        when(slots.findById("NEW")).thenReturn(Optional.of(newSlot));

        assertThatThrownBy(() -> service.decide("SUG", SuggestionStatus.ACCEPTED))
                .isInstanceOf(ConflictException.class);
        assertThat(oldSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void concurrentAcceptsAllowOnlyOnePendingDecision() throws Exception {
        var first = suggestion();
        var second = suggestion();
        when(suggestions.findById("SUG")).thenReturn(Optional.of(first), Optional.of(second));
        when(slots.findById("NEW")).thenReturn(Optional.of(newSlot));
        when(appointments.findById("A1")).thenReturn(Optional.of(appointment));
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var firstResult = executor.submit(() -> { start.await(); return service.decide("SUG", SuggestionStatus.ACCEPTED); });
        var secondResult = executor.submit(() -> { start.await(); return service.decide("SUG", SuggestionStatus.ACCEPTED); });
        start.countDown();
        int accepted = 0;
        try { firstResult.get(); accepted++; } catch (Exception ignored) { }
        try { secondResult.get(); accepted++; } catch (Exception ignored) { }
        executor.shutdownNow();
        assertThat(accepted).isEqualTo(1);
    }

    private RescheduleSuggestion suggestion() {
        var suggestion = new RescheduleSuggestion(appointment, newSlot, provider, 1.0, "reason",
                SuggestionStatus.PENDING, TriggerReason.INITIAL);
        // The repository mock is keyed by the fixed test ID used by this fixture.
        try {
            var field = RescheduleSuggestion.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(suggestion, "SUG");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        return suggestion;
    }
}