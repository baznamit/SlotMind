package com.slotmind.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import java.util.UUID;

@Entity
@Table(name = "reschedule_suggestions")
public class RescheduleSuggestion {
    @Id private String id;
    @ManyToOne(optional = false) private Appointment appointment;
    @ManyToOne(optional = false) private Slot recommendedSlot;
    @ManyToOne private Provider alternateProvider;
    private Double confidence;
    private String reasoning;
    @Enumerated(EnumType.STRING) private SuggestionStatus status;
    @Enumerated(EnumType.STRING) private TriggerReason triggerReason;
    @Column(unique = true) private String pendingKey;
    @Version private long version;

    protected RescheduleSuggestion() { }
    public RescheduleSuggestion(Appointment appointment, Slot recommendedSlot, Provider alternateProvider,
                                Double confidence, String reasoning, SuggestionStatus status, TriggerReason triggerReason) {
        this.id = UUID.randomUUID().toString(); this.appointment = appointment; this.recommendedSlot = recommendedSlot;
        this.alternateProvider = alternateProvider; this.confidence = confidence; this.reasoning = reasoning;
        this.status = status; this.triggerReason = triggerReason;
        this.pendingKey = status == SuggestionStatus.PENDING ? appointment.getId() + "|" + triggerReason : null;
    }
    public String getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public Slot getRecommendedSlot() { return recommendedSlot; }
    public Provider getAlternateProvider() { return alternateProvider; }
    public Double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public void accept() { this.status = SuggestionStatus.ACCEPTED; this.pendingKey = null; }
    public void reject() { this.status = SuggestionStatus.REJECTED; this.pendingKey = null; }
}