package io.walloop.engine.lightning;

import java.util.UUID;

public record LightningInvoiceRequest(UUID processId, UUID ownerId, Long amountMsats) {
}

