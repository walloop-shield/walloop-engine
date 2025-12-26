package com.walloop.engine.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationServiceImpl implements FeeCalculationService {

    @Override
    public long calculateFee(UUID transactionId, UUID ownerId) {
        log.info(
                "Calculating fees via webservice (placeholder): transactionId={} ownerId={}",
                transactionId,
                ownerId);
        return 0L;
    }
}
