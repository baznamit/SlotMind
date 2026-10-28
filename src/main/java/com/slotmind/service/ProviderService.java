package com.slotmind.service;

import com.slotmind.api.AppointmentDtos.ProviderResponse;
import com.slotmind.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.slotmind.domain.ProviderStatus;
import com.slotmind.workflow.ProviderOfflineEvent;
import java.util.List;

@Service
public class ProviderService {
    private final ProviderRepository providers;
    private final ApplicationEventPublisher events;
    public ProviderService(ProviderRepository providers, ApplicationEventPublisher events) { this.providers = providers; this.events = events; }

    @Transactional
    public ProviderResponse updateStatus(String id, ProviderStatus status) {
        var provider = providers.findById(id).orElseThrow(() -> new NotFoundException("Provider not found: " + id));
        var changed = provider.getStatus() != status;
        provider.setStatus(status);
        if (changed && status == ProviderStatus.OFFLINE) events.publishEvent(new ProviderOfflineEvent(id));
        return new ProviderResponse(provider.getId(), provider.getName(), provider.getSpecialty().name(), provider.getStatus());
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> list() {
        return providers.findAll().stream()
                .map(provider -> new ProviderResponse(provider.getId(), provider.getName(),
                        provider.getSpecialty().name(), provider.getStatus()))
                .toList();
    }
}