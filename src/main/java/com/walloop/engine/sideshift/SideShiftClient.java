package com.walloop.engine.sideshift;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sideShiftClient", url = "${sideshift.base-url}")
public interface SideShiftClient {

    @PostMapping(value = "/shift", consumes = MediaType.APPLICATION_JSON_VALUE)
    SideShiftShiftResponse createShift(@RequestBody SideShiftShiftRequest request);
}

