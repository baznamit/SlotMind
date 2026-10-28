package com.slotmind.scheduling;

import com.slotmind.domain.Appointment;
import com.slotmind.domain.Provider;
import com.slotmind.domain.Slot;
import com.slotmind.domain.TriggerReason;
import java.util.List;

public record SchedulingContext(Appointment appointment, List<Provider> eligibleProviders,
                                List<Slot> availableSlots, TriggerReason triggerReason) { }