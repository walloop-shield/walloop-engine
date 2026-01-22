package com.walloop.engine.lightning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(LspLiquidityService.class)
@Slf4j
public class NoopLspLiquidityService implements LspLiquidityService {

    @Override
    public LspLiquidityResponse requestInboundLiquidity(LspLiquidityRequest request) {
        log.warn("LSP disabled, inbound liquidity request skipped processId={}", request.processId());
        return new LspLiquidityResponse(null, null, null);
    }
}
