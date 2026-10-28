package com.slotmind.api;

import com.slotmind.api.AppointmentDtos.AppointmentResponse;
import com.slotmind.domain.AppointmentStatus;
import com.slotmind.service.AppointmentService;
import com.slotmind.service.SuggestionService;
import com.slotmind.api.SuggestionDtos.SuggestionResponse;
import com.slotmind.domain.TriggerReason;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final SuggestionService suggestionService;
    public AppointmentController(AppointmentService service, SuggestionService suggestionService) {
        this.service = service; this.suggestionService = suggestionService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentDtos.CreateAppointmentRequest request) { return service.create(request); }
    @GetMapping
    public List<AppointmentResponse> list(@RequestParam(required = false) AppointmentStatus status) { return service.list(status); }
    @PostMapping("/{id}/suggest")
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestionResponse suggest(@PathVariable String id,
                                      @RequestParam(defaultValue = "INITIAL") TriggerReason triggerReason) {
        return suggestionService.generate(id, triggerReason);
    }
}