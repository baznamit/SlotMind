package com.slotmind.scheduling;

import com.slotmind.domain.Provider;
import com.slotmind.domain.Slot;

public record SchedulingRecommendation(Slot slot, Provider provider, double confidence, String reasoning) { }