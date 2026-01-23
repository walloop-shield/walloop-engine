package com.walloop.engine.boltz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoltzSwapFeeService {

    private static final String FROM_ASSET = "L-BTC";
    private static final String TO_ASSET = "BTC";

    private final BoltzClient boltzClient;

    public FeeQuote quoteInvoice(long balanceSats) {
        Optional<BoltzSubmarinePair> pair = resolveSubmarinePair();
        if (pair.isEmpty()) {
            throw new IllegalStateException("Boltz submarine fees not available");
        }

        BoltzSubmarinePair submarinePair = pair.get();
        BoltzSwapFees fees = submarinePair.fees();
        if (fees == null) {
            throw new IllegalStateException("Boltz submarine fees not available");
        }

        long minerFees = fees.minerFees() == null ? 0 : fees.minerFees();
        double percentage = fees.percentage() == null ? 0.0 : fees.percentage();
        if (balanceSats <= minerFees) {
            throw new IllegalStateException("Liquid balance below Boltz miner fees");
        }

        BigDecimal balance = BigDecimal.valueOf(balanceSats);
        BigDecimal feePercent = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal denominator = BigDecimal.ONE.add(feePercent);
        BigDecimal invoice = balance.subtract(BigDecimal.valueOf(minerFees)).divide(denominator, 0, RoundingMode.DOWN);

        long invoiceSats = invoice.longValue();
        if (invoiceSats <= 0) {
            throw new IllegalStateException("Liquid balance insufficient after Boltz fees");
        }

        BoltzSwapLimits limits = submarinePair.limits();
        if (limits != null) {
            if (limits.minimal() != null && invoiceSats < limits.minimal()) {
                throw new IllegalStateException("Invoice amount below Boltz minimum");
            }
            if (limits.maximal() != null && invoiceSats > limits.maximal()) {
                throw new IllegalStateException("Invoice amount above Boltz maximum");
            }
        }

        return new FeeQuote(invoiceSats, percentage, minerFees, submarinePair.hash());
    }

    private Optional<BoltzSubmarinePair> resolveSubmarinePair() {
        try {
            Map<String, Map<String, BoltzSubmarinePair>> pairs = boltzClient.getSubmarinePairs();
            if (pairs == null) {
                return Optional.empty();
            }
            Map<String, BoltzSubmarinePair> fromMap = pairs.get(FROM_ASSET);
            if (fromMap == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(fromMap.get(TO_ASSET));
        } catch (Exception e) {
            log.warn("Failed to fetch Boltz submarine fees", e);
            return Optional.empty();
        }
    }

    public record FeeQuote(long invoiceSats, double percentage, long minerFees, String hash) {
    }
}
