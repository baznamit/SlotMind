package com.slotmind.scheduling;

import com.slotmind.service.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SchedulingStrategyRegistry {
    private final Map<String, SchedulingStrategy> strategies;
    private final String configuredStrategy;

    public SchedulingStrategyRegistry(Map<String, SchedulingStrategy> strategies,
                                      @Value("${slotmind.scheduling.strategy:rule-based}") String configuredStrategy) {
        this.strategies = strategies; this.configuredStrategy = configuredStrategy;
    }

    public SchedulingStrategy selected() {
        var key = configuredStrategy.equalsIgnoreCase("llm") ? "llmSchedulingStrategy" : "ruleBasedSchedulingStrategy";
        var strategy = strategies.get(key);
        if (strategy == null) throw new ConflictException("Scheduling strategy is not available: " + configuredStrategy);
        return strategy;
    }
}