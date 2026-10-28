package com.slotmind.api;

import com.slotmind.domain.AppointmentStatus;
import com.slotmind.domain.AppointmentType;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.domain.SlotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class AppointmentDtos {
    private AppointmentDtos() { }
    public record CreateAppointmentRequest(@NotBlank String patientName, @NotNull AppointmentType appointmentType,
                                           @NotBlank String providerId, @NotBlank String slotId) { }
    public record AppointmentResponse(String id, String patientName, AppointmentType appointmentType,
                                      String providerId, String slotId, AppointmentStatus status,
                                      LocalDateTime slotStartTime) { }
    public record ProviderResponse(String id, String name, String specialty, ProviderStatus status) { }
    public record SlotResponse(String id, LocalDateTime startTime, int durationMinutes,
                               String providerId, SlotStatus status) { }
}