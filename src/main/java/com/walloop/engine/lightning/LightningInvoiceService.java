package com.walloop.engine.lightning;

import java.util.UUID;

public interface LightningInvoiceService {
    String createOrGetInvoice(UUID processId, UUID ownerId);
}
