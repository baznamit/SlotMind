package com.slotmind.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id private String id;
    private String patientName;
    @Enumerated(EnumType.STRING) private AppointmentType appointmentType;
    @ManyToOne(optional = false) private Provider provider;
    @ManyToOne(optional = false) private Slot slot;
    @Enumerated(EnumType.STRING) private AppointmentStatus status;
    @Version private long version;

    protected Appointment() { }

    public Appointment(String id, String patientName, AppointmentType appointmentType,
                       Provider provider, Slot slot, AppointmentStatus status) {
        this.id = id == null ? UUID.randomUUID().toString() : id; this.patientName = patientName; this.appointmentType = appointmentType;
        this.provider = provider; this.slot = slot; this.status = status;
    }
    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public AppointmentType getAppointmentType() { return appointmentType; }
    public Provider getProvider() { return provider; }
    public Slot getSlot() { return slot; }
    public AppointmentStatus getStatus() { return status; }
    public void rescheduleTo(Provider provider, Slot slot) {
        this.provider = provider;
        this.slot = slot;
        this.status = AppointmentStatus.RESCHEDULED;
    }
    public void markReschedulePending() {
        if (status == AppointmentStatus.CONFIRMED) status = AppointmentStatus.RESCHEDULE_PENDING;
    }
}