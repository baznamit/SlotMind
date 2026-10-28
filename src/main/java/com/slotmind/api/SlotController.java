package com.slotmind.api;

import com.slotmind.api.AppointmentDtos.SlotResponse;
import com.slotmind.domain.SlotStatus;
import com.slotmind.service.SlotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/slots")
public class SlotController {
    private final SlotService service;
    public SlotController(SlotService service) { this.service = service; }
    public record StatusRequest(@NotNull SlotStatus status) { }
    @GetMapping
    public List<SlotResponse> list() { return service.list(); }
    @PatchMapping("/{id}/status")
    public SlotResponse update(@PathVariable String id, @Valid @RequestBody StatusRequest request) { return service.updateStatus(id, request.status()); }
}