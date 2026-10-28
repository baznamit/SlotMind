package com.slotmind.service;

import com.slotmind.api.AppointmentDtos.AppointmentResponse;
import com.slotmind.api.AppointmentDtos.CreateAppointmentRequest;
import com.slotmind.domain.Appointment;
import com.slotmind.domain.AppointmentStatus;
import com.slotmind.domain.SlotStatus;
import com.slotmind.repository.AppointmentRepository;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final SlotRepository slots;

    public AppointmentService(AppointmentRepository appointments, ProviderRepository providers, SlotRepository slots) {
        this.appointments = appointments; this.providers = providers; this.slots = slots;
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        var provider = providers.findById(request.providerId())
                .orElseThrow(() -> new NotFoundException("Provider not found: " + request.providerId()));
        var slot = slots.findById(request.slotId())
                .orElseThrow(() -> new NotFoundException("Slot not found: " + request.slotId()));
        if (!slot.getProvider().getId().equals(provider.getId()))
            throw new ConflictException("Slot does not belong to provider");
        if (slot.getStatus() != SlotStatus.OPEN)
            throw new ConflictException("Slot is not open");
        slot.setStatus(SlotStatus.BOOKED);
        var appointment = appointments.save(new Appointment(null, request.patientName(), request.appointmentType(),
                provider, slot, AppointmentStatus.CONFIRMED));
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(AppointmentStatus status) {
        var values = status == null ? appointments.findAll() : appointments.findByStatus(status);
        return values.stream().map(this::toResponse).toList();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getPatientName(), a.getAppointmentType(),
                a.getProvider().getId(), a.getSlot().getId(), a.getStatus(), a.getSlot().getStartTime());
    }
}