package io.walloop.engine.lightning;

import java.util.UUID;

public interface LightningInvoiceService {
    String createOrGetInvoice(UUID processId, UUID ownerId);

    long resolveInvoiceAmountSats(UUID processId, UUID ownerId);
}

