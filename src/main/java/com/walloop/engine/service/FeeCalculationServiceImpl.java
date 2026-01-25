package com.walloop.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.core.DepositWatchEntity;
import com.walloop.engine.core.DepositWatchRepository;
import com.walloop.engine.core.WithdrawalTransactionRepository;
import com.walloop.engine.fee.FeeCalculationEntity;
import com.walloop.engine.fee.FeeCalculationRepository;
import com.walloop.engine.fixedfloat.FixedFloatOrderEntity;
import com.walloop.engine.fixedfloat.FixedFloatOrderRepository;
import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.fee.FxRateProvider;
import com.walloop.engine.fee.FxRateSnapshot;
import com.walloop.engine.network.NetworkAssetService;
import com.walloop.engine.sideshift.SideShiftPairSimulationEntity;
import com.walloop.engine.sideshift.SideShiftPairSimulationRepository;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationServiceImpl implements FeeCalculationService {

    private static final BigDecimal SATS_PER_BTC = new BigDecimal("100000000");

    private final DepositWatchRepository depositWatchRepository;
    private final SideShiftPairSimulationRepository pairSimulationRepository;
    private final FixedFloatOrderRepository fixedFloatOrderRepository;
    private final FeeCalculationRepository feeCalculationRepository;
    private final WithdrawalTransactionRepository withdrawalTransactionRepository;
    private final NetworkAssetService networkAssetService;
    private final FxRateProvider fxRateProvider;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final LiquidWalletRepository liquidWalletRepository;
    private final ObjectMapper objectMapper;

    @Value("${walloop.fee.platform-percent:0.5}")
    private BigDecimal platformPercent;

    @Value("${walloop.fee.sideshift-percent:0.4}")
    private BigDecimal sideshiftPercent;

    @Value("${walloop.fee.boltz-percent:0.3}")
    private BigDecimal boltzPercent;

    @Value("${walloop.fee.fixedfloat-percent:0.6}")
    private BigDecimal fixedfloatPercent;

    @Value("#{'${walloop.fee.onchain-chains:BTC,L-BTC,LBTC,LIQUID}'.split(',')}")
    private List<String> onchainChains;

    @Override
    public long calculateFee(UUID transactionId, UUID ownerId) {
        DepositWatchEntity watch = depositWatchRepository.findByProcessId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Deposit watch not found"));

        BigDecimal amountBaseUnits = parseAmountSats(watch.getLastBalance());
        String assetSymbol = resolveAssetSymbol(watch.getNetwork());
        FxRateSnapshot rates = resolveRates(transactionId);
        AmountSnapshot snapshot = resolveAmountSnapshot(amountBaseUnits, assetSymbol, watch.getNetwork(), rates);
        BigDecimal amountSats = snapshot.amountSats();
        BigDecimal amountBtc = snapshot.amountBtc();
        BigDecimal amountUsd = snapshot.amountUsd();
        BigDecimal amountBrl = snapshot.amountBrl();

        BigDecimal feePercent = resolveCheapestPercent();
        BigDecimal feeSats = amountSats.multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        BigDecimal feeBtc = feeSats.divide(SATS_PER_BTC, 12, RoundingMode.DOWN);
        BigDecimal feeUsd = amountUsd != null
                ? amountUsd.multiply(feePercent).divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN)
                : null;
        BigDecimal feeBrl = amountBrl != null
                ? amountBrl.multiply(feePercent).divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN)
                : null;

        long onchainFeeSats = resolveOnchainFeeSats(transactionId);
        BigDecimal onchainFeeBtc = BigDecimal.valueOf(onchainFeeSats).divide(SATS_PER_BTC, 12, RoundingMode.DOWN);
        BigDecimal onchainFeeUsd = amountUsd != null && rates.btcUsd() != null
                ? onchainFeeBtc.multiply(rates.btcUsd()).setScale(8, RoundingMode.DOWN)
                : null;
        BigDecimal onchainFeeBrl = amountBrl != null && onchainFeeUsd != null && rates.usdBrl() != null
                ? onchainFeeUsd.multiply(rates.usdBrl()).setScale(8, RoundingMode.DOWN)
                : null;

        long totalFeeSats = feeSats.longValue() + onchainFeeSats;
        BigDecimal totalFeeBtc = feeBtc.add(onchainFeeBtc);
        BigDecimal totalFeeUsd = feeUsd != null && onchainFeeUsd != null ? feeUsd.add(onchainFeeUsd) : feeUsd;
        BigDecimal totalFeeBrl = feeBrl != null && onchainFeeBrl != null ? feeBrl.add(onchainFeeBrl) : feeBrl;

        FeeCalculationEntity entity = new FeeCalculationEntity();
        entity.setProcessId(transactionId);
        entity.setOwnerId(ownerId);
        entity.setAmountSats(amountSats.longValue());
        entity.setAmountBtc(amountBtc);
        entity.setAmountUsd(amountUsd);
        entity.setAmountBrl(amountBrl);
        entity.setFeePercent(feePercent);
        entity.setFeeSats(feeSats.longValue());
        entity.setOnchainFeeSats(onchainFeeSats);
        entity.setTotalFeeSats(totalFeeSats);
        entity.setFeeBtc(feeBtc);
        entity.setFeeUsd(feeUsd);
        entity.setFeeBrl(feeBrl);
        entity.setTotalFeeBtc(totalFeeBtc);
        entity.setTotalFeeUsd(totalFeeUsd);
        entity.setTotalFeeBrl(totalFeeBrl);
        entity.setPayload(buildPayload(transactionId, ownerId));
        entity.setCreatedAt(OffsetDateTime.now());
        feeCalculationRepository.save(entity);

        log.info(
                "FeeCalculationServiceImpl - Fee calculated processId={} percent={} sats={} onchainSats={} totalSats={}",
                transactionId,
                feePercent,
                feeSats,
                onchainFeeSats,
                totalFeeSats
        );
        return totalFeeSats;
    }

    private BigDecimal resolveCheapestPercent() {
        List<BigDecimal> candidates = new ArrayList<>();
        addCandidate(candidates, platformPercent);
        addCandidate(candidates, sideshiftPercent);
        addCandidate(candidates, boltzPercent);
        addCandidate(candidates, fixedfloatPercent);
        return candidates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    private void addCandidate(List<BigDecimal> candidates, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            candidates.add(value);
        }
    }

    private FxRateSnapshot resolveRates(UUID processId) {
        BigDecimal btcUsd = resolveUsdRate(processId);
        BigDecimal usdBrl = resolveUsdBrlRate(btcUsd);
        return new FxRateSnapshot(btcUsd, usdBrl);
    }

    private BigDecimal resolveUsdRate(UUID processId) {
        Optional<SideShiftPairSimulationEntity> simulation = pairSimulationRepository
                .findFirstByProcessIdOrderByCreatedAtDesc(processId);
        if (simulation.isPresent()) {
            String rateValue = simulation.get().getRate();
            String fromCoin = simulation.get().getFromCoin();
            String toCoin = simulation.get().getToCoin();
            if (rateValue != null && !rateValue.isBlank() && fromCoin != null && toCoin != null) {
                BigDecimal rate = new BigDecimal(rateValue);
                if ("btc".equalsIgnoreCase(fromCoin) && "usdt".equalsIgnoreCase(toCoin)) {
                    return rate;
                }
                if ("usdt".equalsIgnoreCase(fromCoin) && "btc".equalsIgnoreCase(toCoin)) {
                    return BigDecimal.ONE.divide(rate, 8, RoundingMode.DOWN);
                }
            }
        }

        Optional<FixedFloatOrderEntity> order = fixedFloatOrderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId);
        if (order.isPresent() && order.get().getAmount() != null) {
            if ("USDT".equalsIgnoreCase(order.get().getToCcy())) {
                BigDecimal amountBtc = new BigDecimal(order.get().getAmount());
                return amountBtc.compareTo(BigDecimal.ZERO) > 0 ? amountBtc : null;
            }
        }

        Optional<FxRateSnapshot> providerRates = fxRateProvider.fetch();
        if (providerRates.isPresent() && providerRates.get().btcUsd() != null) {
            return providerRates.get().btcUsd();
        }

        return null;
    }

    private BigDecimal resolveUsdBrlRate(BigDecimal btcUsd) {
        Optional<FxRateSnapshot> providerRates = fxRateProvider.fetch();
        if (providerRates.isPresent() && providerRates.get().usdBrl() != null) {
            return providerRates.get().usdBrl();
        }
        return null;
    }

    private AmountSnapshot resolveAmountSnapshot(
            BigDecimal amountBaseUnits,
            String assetSymbol,
            String network,
            FxRateSnapshot rates
    ) {
        if (isBtcAsset(assetSymbol)) {
            BigDecimal amountBtc = amountBaseUnits.divide(SATS_PER_BTC, 12, RoundingMode.DOWN);
            BigDecimal amountUsd = rates.btcUsd() != null
                    ? amountBtc.multiply(rates.btcUsd()).setScale(8, RoundingMode.DOWN)
                    : null;
            BigDecimal amountBrl = amountUsd != null && rates.usdBrl() != null
                    ? amountUsd.multiply(rates.usdBrl()).setScale(8, RoundingMode.DOWN)
                    : null;
            return new AmountSnapshot(amountBaseUnits, amountBtc, amountUsd, amountBrl);
        }

        int decimals = resolveAssetDecimals(network, assetSymbol);
        BigDecimal divisor = BigDecimal.TEN.pow(decimals);
        BigDecimal amountAsset = amountBaseUnits.divide(divisor, 18, RoundingMode.DOWN);
        BigDecimal assetUsd = resolveAssetUsd(network, assetSymbol);
        if (assetUsd == null || assetUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Missing USD rate for asset=" + assetSymbol);
        }
        BigDecimal amountUsd = amountAsset.multiply(assetUsd).setScale(8, RoundingMode.DOWN);
        if (rates.btcUsd() == null || rates.btcUsd().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Missing BTC/USD rate for asset=" + assetSymbol);
        }
        BigDecimal amountBtc = amountUsd.divide(rates.btcUsd(), 12, RoundingMode.DOWN);
        BigDecimal amountSats = amountBtc.multiply(SATS_PER_BTC).setScale(0, RoundingMode.DOWN);
        BigDecimal amountBrl = rates.usdBrl() != null
                ? amountUsd.multiply(rates.usdBrl()).setScale(8, RoundingMode.DOWN)
                : null;
        return new AmountSnapshot(amountSats, amountBtc, amountUsd, amountBrl);
    }

    private String resolveAssetSymbol(String network) {
        if (network == null || network.isBlank()) {
            return "BTC";
        }
        return networkAssetService.findMainAsset(network)
                .orElseGet(() -> network.trim().toUpperCase());
    }

    private boolean isBtcAsset(String assetSymbol) {
        if (assetSymbol == null) {
            return true;
        }
        String normalized = assetSymbol.trim().toUpperCase();
        return "BTC".equals(normalized)
                || "LBTC".equals(normalized)
                || "L-BTC".equals(normalized)
                || "LIQUID".equals(normalized);
    }

    private int resolveAssetDecimals(String network, String assetSymbol) {
        if (isBtcAsset(assetSymbol)) {
            return 8;
        }
        return networkAssetService.findAsset(network)
                .map(asset -> asset.decimals() == null ? 18 : asset.decimals())
                .orElse(18);
    }

    private BigDecimal resolveAssetUsd(String network, String assetSymbol) {
        if (isBtcAsset(assetSymbol)) {
            return fxRateProvider.fetchAssetUsd("bitcoin").orElse(null);
        }
        return networkAssetService.findAsset(network)
                .map(asset -> asset.priceId())
                .filter(id -> id != null && !id.isBlank())
                .flatMap(id -> fxRateProvider.fetchAssetUsd(id))
                .orElse(null);
    }

    private long resolveOnchainFeeSats(UUID processId) {
        List<String> normalized = onchainChains.stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase())
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return 0L;
        }
        BigInteger feeWei = withdrawalTransactionRepository.sumFeeWeiByProcessId(processId, normalized);
        if (feeWei == null) {
            return 0L;
        }
        if (feeWei.compareTo(BigInteger.ZERO) <= 0) {
            return 0L;
        }
        return feeWei.longValue();
    }

    private record AmountSnapshot(
            BigDecimal amountSats,
            BigDecimal amountBtc,
            BigDecimal amountUsd,
            BigDecimal amountBrl
    ) {
    }

    private BigDecimal parseAmountSats(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Deposit watch lastBalance not available");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid deposit watch lastBalance: " + value, e);
        }
    }

    private String buildPayload(UUID processId, UUID ownerId) {
        Map<String, Object> payload = new HashMap<>();
        walletTransactionQueryService.find(processId, ownerId)
                .ifPresent(tx -> payload.put("context", buildContext(tx)));
        liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                .ifPresent(wallet -> payload.put("liquidWallet", buildLiquidWalletPayload(wallet)));
        pairSimulationRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .ifPresent(simulation -> payload.put("sideshiftPair", simulation));
        fixedFloatOrderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .ifPresent(order -> payload.put("fixedfloatOrder", order));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildContext(WalletTransactionDetails tx) {
        Map<String, Object> context = new HashMap<>();
        context.put("chain", tx.chain());
        context.put("correlatedAddress", tx.correlatedAddress());
        context.put("transactionAddress", tx.newAddress());
        context.put("destinationAddress", tx.newAddress2());
        return context;
    }

    private Map<String, Object> buildLiquidWalletPayload(LiquidWalletEntity wallet) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("address", wallet.getAddress());
        payload.put("label", wallet.getLabel());
        return payload;
    }
}
