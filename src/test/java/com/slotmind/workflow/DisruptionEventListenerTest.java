package com.slotmind.workflow;

import com.slotmind.domain.*;
import com.slotmind.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisruptionEventListenerTest {
    @Mock AppointmentRepository appointments;
    @Mock DisruptionProcessor processor;

    @Test
    void providerOfflineProcessesAllConfirmedAppointments() {
        var first = appointment("A1", AppointmentStatus.CONFIRMED);
        var second = appointment("A2", AppointmentStatus.CONFIRMED);
        when(appointments.findByProvider_IdAndStatus("P1", AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(first, second));
        var listener = new DisruptionEventListener(appointments, processor);

        listener.providerOffline(new ProviderOfflineEvent("P1"));

        verify(processor).process("A1", TriggerReason.PROVIDER_OFFLINE);
        verify(processor).process("A2", TriggerReason.PROVIDER_OFFLINE);
    }

    @Test
    void slotCancelledProcessesAssignedConfirmedAppointment() {
        when(appointments.findBySlot_Id("S1")).thenReturn(Optional.of(appointment("A1", AppointmentStatus.CONFIRMED)));
        var listener = new DisruptionEventListener(appointments, processor);

        listener.slotCancelled(new SlotCancelledEvent("S1"));

        verify(processor).process("A1", TriggerReason.SLOT_CANCELLED);
    }

    @Test
    void oneAppointmentFailureDoesNotStopTheOthers() {
        var first = appointment("A1", AppointmentStatus.CONFIRMED);
        var second = appointment("A2", AppointmentStatus.CONFIRMED);
        when(appointments.findByProvider_IdAndStatus("P1", AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("expected test failure")).when(processor)
                .process("A1", TriggerReason.PROVIDER_OFFLINE);
        var listener = new DisruptionEventListener(appointments, processor);

        listener.providerOffline(new ProviderOfflineEvent("P1"));

        verify(processor).process("A2", TriggerReason.PROVIDER_OFFLINE);
    }

    @Test
    void listenerIsPostCommitAndAsync() throws Exception {
        var providerMethod = DisruptionEventListener.class.getMethod("providerOffline", ProviderOfflineEvent.class);
        var slotMethod = DisruptionEventListener.class.getMethod("slotCancelled", SlotCancelledEvent.class);

        assertThat(providerMethod.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isTrue();
        assertThat(providerMethod.isAnnotationPresent(org.springframework.transaction.event.TransactionalEventListener.class)).isTrue();
        assertThat(slotMethod.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isTrue();
        assertThat(slotMethod.getAnnotation(org.springframework.transaction.event.TransactionalEventListener.class).phase())
                .isEqualTo(org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT);
    }

    private Appointment appointment(String id, AppointmentStatus status) {
        var provider = new Provider("P1", "Provider", Specialty.GENERAL, ProviderStatus.OFFLINE);
        var slot = new Slot("S-" + id, LocalDateTime.now(), 30, provider, SlotStatus.BOOKED);
        return new Appointment(id, "Patient", AppointmentType.ROUTINE, provider, slot, status);
    }
}