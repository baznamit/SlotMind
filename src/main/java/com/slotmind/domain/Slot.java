package com.slotmind.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "slots")
public class Slot {
    @Id private String id;
    private LocalDateTime startTime;
    private int durationMinutes;
    @ManyToOne(optional = false) private Provider provider;
    @Enumerated(EnumType.STRING) private SlotStatus status;
    @Version private long version;

    protected Slot() { }

    public Slot(String id, LocalDateTime startTime, int durationMinutes, Provider provider, SlotStatus status) {
        this.id = id; this.startTime = startTime; this.durationMinutes = durationMinutes;
        this.provider = provider; this.status = status;
    }
    public String getId() { return id; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public Provider getProvider() { return provider; }
    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }
}