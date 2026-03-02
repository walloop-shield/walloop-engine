package io.walloop.engine.fixedfloat;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "fixedFloatClient", url = "${fixedfloat.base-url}")
public interface FixedFloatClient {

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    FixedFloatResponse<Map<String, Object>> createOrder(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestHeader("X-API-SIGN") String apiSign,
            @RequestBody String payload
    );

    @PostMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE)
    FixedFloatResponse<Map<String, Object>> getOrder(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestHeader("X-API-SIGN") String apiSign,
            @RequestBody String payload
    );

    @PostMapping(value = "/price", consumes = MediaType.APPLICATION_JSON_VALUE)
    FixedFloatResponse<Map<String, Object>> getPrice(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestHeader("X-API-SIGN") String apiSign,
            @RequestBody String payload
    );
}

