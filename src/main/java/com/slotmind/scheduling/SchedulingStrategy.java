package com.slotmind.scheduling;

import java.util.List;

public interface SchedulingStrategy {
    List<SchedulingRecommendation> recommend(SchedulingContext context);
}