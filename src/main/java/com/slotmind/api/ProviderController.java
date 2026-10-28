package com.slotmind.api;

import com.slotmind.api.AppointmentDtos.ProviderResponse;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.service.ProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/providers")
public class ProviderController {
    private final ProviderService service;
    public ProviderController(ProviderService service) { this.service = service; }
    public record StatusRequest(@NotNull ProviderStatus status) { }
    @GetMapping
    public List<ProviderResponse> list() { return service.list(); }
    @PatchMapping("/{id}/status")
    public ProviderResponse update(@PathVariable String id, @Valid @RequestBody StatusRequest request) { return service.updateStatus(id, request.status()); }
}