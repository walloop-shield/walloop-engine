package com.walloop.engine.lightning;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LightningInvoiceServiceImpl implements LightningInvoiceService {

    private final LightningInvoiceRepository repository;
    private final LightningInvoiceClient client;

    @Override
    public String createOrGetInvoice(UUID processId, UUID ownerId) {
        return repository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .map(LightningInvoiceEntity::getInvoice)
                .orElseGet(() -> createInvoice(processId, ownerId));
    }

    private String createInvoice(UUID processId, UUID ownerId) {
        LightningInvoiceResponse response = client.createInvoice(new LightningInvoiceRequest(processId, ownerId));
        if (response == null || response.invoice() == null || response.invoice().isBlank()) {
            throw new IllegalStateException("Lightning invoice not returned by provider");
        }

        LightningInvoiceEntity entity = new LightningInvoiceEntity();
        entity.setProcessId(processId);
        entity.setOwnerId(ownerId);
        entity.setInvoice(response.invoice());
        entity.setStatus(LightningInvoiceStatus.CREATED);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
        return response.invoice();
    }
}
