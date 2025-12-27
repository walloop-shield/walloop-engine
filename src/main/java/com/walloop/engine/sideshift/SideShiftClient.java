package com.walloop.engine.sideshift;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "sideShiftClient", url = "${sideshift.base-url}")
public interface SideShiftClient {

    @PostMapping(value = "/shifts/variable", consumes = MediaType.APPLICATION_JSON_VALUE)
    SideShiftShiftResponse createVariableShift(
            @org.springframework.web.bind.annotation.RequestHeader("x-sideshift-secret") String secret,
            @org.springframework.web.bind.annotation.RequestHeader(value = "x-user-ip", required = false) String userIp,
            @RequestBody SideShiftCreateVariableShiftRequest request
    );

    @GetMapping("/pair")
    java.util.Map<String, Object> getPair(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("amount") String amount,
            @RequestParam(value = "depositNetwork", required = false) String depositNetwork,
            @RequestParam(value = "settleNetwork", required = false) String settleNetwork
    );

    @GetMapping("/shifts/{shiftId}")
    SideShiftShiftStatusResponse getShift(
            @org.springframework.web.bind.annotation.RequestHeader("x-sideshift-secret") String secret,
            @org.springframework.web.bind.annotation.RequestHeader(value = "x-user-ip", required = false) String userIp,
            @PathVariable("shiftId") String shiftId
    );
}
