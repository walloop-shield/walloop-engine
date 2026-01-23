package com.walloop.engine.boltz;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "boltzClient", url = "${boltz.base-url}")
public interface BoltzClient {

    @PostMapping(value = "/swap/submarine", consumes = MediaType.APPLICATION_JSON_VALUE)
    BoltzSubmarineResponse createSubmarineSwap(@RequestBody BoltzSubmarineRequest request);

    @GetMapping(value = "/swap/submarine", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Map<String, BoltzSubmarinePair>> getSubmarinePairs();

    @GetMapping("/swap/{id}")
    BoltzSwapStatusResponse getSwapStatus(@PathVariable("id") String id);
}
