package com.slotmind.api;

import com.slotmind.api.SuggestionDtos.DecisionRequest;
import com.slotmind.api.SuggestionDtos.SuggestionResponse;
import com.slotmind.domain.TriggerReason;
import com.slotmind.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.slotmind.domain.SuggestionStatus;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {
    private final SuggestionService service;
    public SuggestionController(SuggestionService service) { this.service = service; }
    @PatchMapping("/{id}")
    public SuggestionResponse decide(@PathVariable String id, @Valid @RequestBody DecisionRequest request) {
        return service.decide(id, request.decision());
    }
    @GetMapping
    public List<SuggestionResponse> list(@RequestParam(defaultValue = "PENDING") SuggestionStatus status) {
        return service.list(status);
    }
}