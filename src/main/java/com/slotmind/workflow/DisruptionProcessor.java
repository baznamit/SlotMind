package com.slotmind.workflow;

import com.slotmind.domain.TriggerReason;
import com.slotmind.service.SuggestionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DisruptionProcessor {
    private final SuggestionService suggestions;

    public DisruptionProcessor(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @Transactional
    public void process(String appointmentId, TriggerReason triggerReason) {
        suggestions.generate(appointmentId, triggerReason);
    }
}