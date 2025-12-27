package com.walloop.engine.lightning;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lightningInvoiceClient", url = "${clients.lightning.url}")
public interface LightningInvoiceClient {

    @PostMapping(value = "/invoice", consumes = MediaType.APPLICATION_JSON_VALUE)
    LightningInvoiceResponse createInvoice(@RequestBody LightningInvoiceRequest request);
}
