package com.slotmind.service;

import com.slotmind.api.SuggestionDtos.SuggestionResponse;
import com.slotmind.domain.AppointmentStatus;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.domain.SlotStatus;
import com.slotmind.domain.SuggestionStatus;
import com.slotmind.domain.TriggerReason;
import com.slotmind.repository.AppointmentRepository;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.repository.RescheduleSuggestionRepository;
import com.slotmind.repository.SlotRepository;
import com.slotmind.scheduling.RuleBasedSchedulingStrategy;
import com.slotmind.scheduling.SchedulingContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SuggestionService {
    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final SlotRepository slots;
    private final RescheduleSuggestionRepository suggestions;
    private final com.slotmind.scheduling.SchedulingStrategyRegistry strategyRegistry;

    public SuggestionService(AppointmentRepository appointments, ProviderRepository providers, SlotRepository slots,
                             RescheduleSuggestionRepository suggestions, com.slotmind.scheduling.SchedulingStrategyRegistry strategyRegistry) {
        this.appointments = appointments; this.providers = providers; this.slots = slots;
        this.suggestions = suggestions; this.strategyRegistry = strategyRegistry;
    }

    @Transactional
    public SuggestionResponse generate(String appointmentId, TriggerReason triggerReason) {
        var appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));
        var existing = suggestions.findByAppointment_IdAndTriggerReasonAndStatus(appointmentId, triggerReason, SuggestionStatus.PENDING);
        if (existing.isPresent()) return toResponse(existing.get());
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.RESCHEDULE_PENDING)
            throw new ConflictException("Appointment cannot be rescheduled in status " + appointment.getStatus());
        if (triggerReason != TriggerReason.INITIAL) appointment.markReschedulePending();
        var context = new SchedulingContext(appointment, providers.findAll(), slots.findAll(), triggerReason);
        var recommendation = strategyRegistry.selected().recommend(context).stream().findFirst()
                .orElseThrow(() -> new NoEligibleSlotException("No eligible open slot exists for this appointment"));
        var suggestion = suggestions.save(new com.slotmind.domain.RescheduleSuggestion(
                appointment, recommendation.slot(), recommendation.provider(), recommendation.confidence(),
                recommendation.reasoning(), SuggestionStatus.PENDING, triggerReason));
        return toResponse(suggestion);
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> list(SuggestionStatus status) {
        return suggestions.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SuggestionResponse decide(String suggestionId, SuggestionStatus decision) {
        if (decision != SuggestionStatus.ACCEPTED && decision != SuggestionStatus.REJECTED)
            throw new ConflictException("Decision must be ACCEPTED or REJECTED");
        var suggestion = suggestions.findById(suggestionId)
                .orElseThrow(() -> new NotFoundException("Suggestion not found: " + suggestionId));
        if (suggestion.getStatus() != SuggestionStatus.PENDING)
            throw new ConflictException("Suggestion is no longer pending");
        if (decision == SuggestionStatus.REJECTED) {
            suggestion.reject();
            return toResponse(suggestion);
        }
        var target = slots.findById(suggestion.getRecommendedSlot().getId())
                .orElseThrow(() -> new NotFoundException("Recommended slot not found"));
        var provider = target.getProvider();
        if (target.getStatus() != SlotStatus.OPEN || provider.getStatus() == ProviderStatus.OFFLINE)
            throw new ConflictException("Recommended slot is no longer available");
        var appointment = appointments.findById(suggestion.getAppointment().getId())
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.RESCHEDULE_PENDING)
            throw new ConflictException("Appointment cannot be rescheduled in status " + appointment.getStatus());
        var oldSlot = appointment.getSlot();
        if (oldSlot.getId().equals(target.getId())) throw new ConflictException("Recommended slot is already assigned");
        if (provider.getSpecialty() != appointment.getProvider().getSpecialty())
            throw new ConflictException("Recommended provider specialty is incompatible");
        if (suggestion.getTriggerReason() == TriggerReason.PROVIDER_OFFLINE
                && provider.getId().equals(appointment.getProvider().getId()))
            throw new ConflictException("Offline provider cannot be recommended");
        if (suggestion.getTriggerReason() == TriggerReason.SLOT_CANCELLED
                && !provider.getId().equals(appointment.getProvider().getId())
                && slots.findAll().stream().anyMatch(candidate -> candidate.getStatus() == SlotStatus.OPEN
                && candidate.getProvider().getId().equals(appointment.getProvider().getId())))
            throw new ConflictException("Original provider has an eligible open slot");
        target.setStatus(SlotStatus.BOOKED);
        if (oldSlot.getStatus() == SlotStatus.BOOKED) oldSlot.setStatus(SlotStatus.OPEN);
        appointment.rescheduleTo(provider, target);
        suggestion.accept();
        return toResponse(suggestion);
    }

    private SuggestionResponse toResponse(com.slotmind.domain.RescheduleSuggestion suggestion) {
        var slot = suggestion.getRecommendedSlot();
        return new SuggestionResponse(suggestion.getId(), suggestion.getAppointment().getId(), slot.getId(),
                slot.getProvider().getId(), suggestion.getConfidence(), suggestion.getReasoning(), suggestion.getStatus(),
                suggestion.getTriggerReason(), slot.getStartTime());
    }
}