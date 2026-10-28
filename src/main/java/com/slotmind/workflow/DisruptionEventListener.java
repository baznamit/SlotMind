package com.slotmind.workflow;

import com.slotmind.domain.AppointmentStatus;
import com.slotmind.domain.TriggerReason;
import com.slotmind.repository.AppointmentRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DisruptionEventListener {
    private final AppointmentRepository appointments;
    private final DisruptionProcessor processor;

    public DisruptionEventListener(AppointmentRepository appointments, DisruptionProcessor processor) {
        this.appointments = appointments;
        this.processor = processor;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void providerOffline(ProviderOfflineEvent event) {
        for (var appointment : appointments.findByProvider_IdAndStatus(event.providerId(), AppointmentStatus.CONFIRMED)) {
            process(appointment.getId(), TriggerReason.PROVIDER_OFFLINE);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void slotCancelled(SlotCancelledEvent event) {
        appointments.findBySlot_Id(event.slotId())
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .ifPresent(appointment -> process(appointment.getId(), TriggerReason.SLOT_CANCELLED));
    }

    private void process(String appointmentId, TriggerReason triggerReason) {
        try {
            processor.process(appointmentId, triggerReason);
        } catch (RuntimeException ignored) {
            // A failed appointment must not prevent other disruption appointments from being processed.
        }
    }
}