package com.walloop.engine.swap;

import java.util.UUID;

public interface SwapQuoteService {

    void ensureQuote(UUID processId, String network, String depositNetwork);
}
