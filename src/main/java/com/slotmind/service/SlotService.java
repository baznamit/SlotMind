package com.slotmind.service;

import com.slotmind.api.AppointmentDtos.SlotResponse;
import com.slotmind.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.slotmind.domain.SlotStatus;
import com.slotmind.workflow.SlotCancelledEvent;
import java.util.List;

@Service
public class SlotService {
    private final SlotRepository slots;
    private final ApplicationEventPublisher events;
    public SlotService(SlotRepository slots, ApplicationEventPublisher events) { this.slots = slots; this.events = events; }

    @Transactional
    public SlotResponse updateStatus(String id, SlotStatus status) {
        var slot = slots.findById(id).orElseThrow(() -> new NotFoundException("Slot not found: " + id));
        var changed = slot.getStatus() != status;
        slot.setStatus(status);
        if (changed && status == SlotStatus.CANCELLED) events.publishEvent(new SlotCancelledEvent(id));
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getDurationMinutes(), slot.getProvider().getId(), slot.getStatus());
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> list() {
        return slots.findAll().stream()
                .map(slot -> new SlotResponse(slot.getId(), slot.getStartTime(), slot.getDurationMinutes(),
                        slot.getProvider().getId(), slot.getStatus()))
                .toList();
    }
}