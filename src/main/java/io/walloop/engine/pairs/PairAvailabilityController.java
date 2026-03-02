package io.walloop.engine.pairs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pairs")
@RequiredArgsConstructor
public class PairAvailabilityController {

    private final PairAvailabilityService pairAvailabilityService;

    @GetMapping("/{network}")
    public ResponseEntity<PairAvailabilityResponse> getPairs(@PathVariable String network) {
        return ResponseEntity.ok(pairAvailabilityService.checkByNetwork(network));
    }
}

