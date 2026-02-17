package com.walloop.engine.api;

import com.walloop.engine.lightning.LightningOutboundLiquidityService;
import com.walloop.engine.lightning.OutboundLiquidityInvoiceRequest;
import com.walloop.engine.lightning.OutboundLiquidityInvoiceResponse;
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
