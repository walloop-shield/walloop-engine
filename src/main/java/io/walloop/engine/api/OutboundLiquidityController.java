package io.walloop.engine.api;

import io.walloop.engine.lightning.LightningOutboundLiquidityService;
import io.walloop.engine.lightning.OutboundLiquidityInvoiceRequest;
import io.walloop.engine.lightning.OutboundLiquidityInvoiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/liquidity/outbound")
public class OutboundLiquidityController {

    private final LightningOutboundLiquidityService outboundLiquidityService;

    @PostMapping("/invoice")
    public OutboundLiquidityInvoiceResponse createInvoice(@Valid @RequestBody OutboundLiquidityInvoiceRequest request) {
        return outboundLiquidityService.createInvoice(request);
    }
}

