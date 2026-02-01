package com.walloop.engine.settlement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.conversion.ConversionOrderEntity;
import com.walloop.engine.conversion.ConversionOrderRepository;
import com.walloop.engine.core.DepositWatchEntity;
import com.walloop.engine.core.DepositWatchRepository;
import com.walloop.engine.core.WithdrawalTransactionEntity;
import com.walloop.engine.core.WithdrawalTransactionRepository;
import com.walloop.engine.explorer.ExplorerUrlResolver;
import com.walloop.engine.fee.FxRateProvider;
import com.walloop.engine.fee.FxRateSnapshot;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.network.NetworkAssetService;
import com.walloop.engine.wallet.dto.NetworkAssetResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessSettlementSnapshotService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal SATS_PER_BTC = new BigDecimal("100000000");

    private final DepositWatchRepository depositWatchRepository;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final ConversionOrderRepository conversionOrderRepository;
    private final WithdrawalTransactionRepository withdrawalTransactionRepository;
    private final FxRateProvider fxRateProvider;
    private final NetworkAssetService networkAssetService;
    private final ExplorerUrlResolver explorerUrlResolver;
    private final ProcessSettlementSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void capture(UUID processId) {
        ProcessSettlementSnapshotEntity entity = new ProcessSettlementSnapshotEntity();
        entity.setProcessId(processId);
        entity.setCreatedAt(OffsetDateTime.now());

        Optional<DepositWatchEntity> watch = depositWatchRepository.findByProcessId(processId);
        Optional<LightningInvoiceEntity> invoice = lightningInvoiceRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId);
        Optional<ConversionOrderEntity> conversion = conversionOrderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId);
        WithdrawalTransactionEntity destinationWithdrawal = resolveDestinationWithdrawal(processId);

        String blockchain = watch.map(DepositWatchEntity::getNetwork)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);

        BigDecimal initialAmount = watch.map(DepositWatchEntity::getLastBalance)
                .map(this::toBigDecimal)
                .orElse(null);

        FxRateSnapshot rates = fxRateProvider.fetch().orElse(null);
        BigDecimal btcUsd = resolveBtcUsd(rates);
        BigDecimal usdBrl = rates != null ? rates.usdBrl() : null;
        BigDecimal btcChain = resolveBtcChainRate(blockchain, btcUsd);
        NetworkAssetResponse nativeAsset = networkAssetService.findAsset(blockchain).orElse(null);

        BigDecimal liquidAmount = normalizeSatsToNative(invoice.map(LightningInvoiceEntity::getSwapExpectedAmount)
                .map(BigDecimal::valueOf)
                .orElse(null), blockchain, nativeAsset, btcUsd);
        BigDecimal liquidFee = normalizeSatsToNative(invoice.map(LightningInvoiceEntity::getLiquidFeeSats)
                .map(BigDecimal::valueOf)
                .orElse(null), blockchain, nativeAsset, btcUsd);
        String liquidTxUrl = explorerUrlResolver.buildTxUrl("liquid",
            invoice.map(LightningInvoiceEntity::getLiquidTxId).orElse(null));

        LightningPayload lightningPayload = resolveLightningPayload(invoice.orElse(null), blockchain, nativeAsset, btcUsd);

        ConversionPayload conversionPayload = resolveConversionPayload(conversion.orElse(null), blockchain, nativeAsset, btcUsd);
        String conversionTxUrl = explorerUrlResolver.buildTxUrl(blockchain, conversionPayload.txId());

        BigDecimal destinationAmount = normalizeWeiToNative(destinationWithdrawal.getAmountWei(), nativeAsset);
        BigDecimal destinationFee = normalizeWeiToNative(destinationWithdrawal.getFeeWei(), nativeAsset);
        String destinationTxUrl = explorerUrlResolver.buildTxUrl(destinationWithdrawal.getChain(), destinationWithdrawal.getTxHash());

        BigDecimal lightningFee = lightningPayload.fee() == null ? BigDecimal.ZERO : lightningPayload.fee();
        BigDecimal feePercent = sum(
                liquidFee,
                lightningFee,
                conversionPayload.fee(),
                destinationFee
        );
        BigDecimal feeAmount = sum(
                liquidAmount,
                lightningPayload.amount(),
                conversionPayload.amount(),
                destinationAmount
        );

        entity.setBlockchain(blockchain);
        entity.setInitialAmountDetected(initialAmount);
        entity.setRateIdxDollarReal(usdBrl);
        entity.setRateIdxBitcoinDollar(btcUsd);
        entity.setRateIdxBitcoinChain(btcChain);
        entity.setLiquidTxUrl(liquidTxUrl);
        entity.setLiquidAmount(liquidAmount);
        entity.setLiquidFee(liquidFee);
        entity.setLightningTxUrl(lightningPayload.txHash());
        entity.setLightningAmount(lightningPayload.amount());
        entity.setLightningFee(lightningFee);
        entity.setConversionTxUrl(conversionTxUrl);
        entity.setConversionAmount(conversionPayload.amount());
        entity.setConversionFee(conversionPayload.fee());
        entity.setDestinationTxUrl(destinationTxUrl);
        entity.setDestinationAmount(destinationAmount);
        entity.setDestinationFee(destinationFee);
        entity.setFeePercent(feePercent);
        entity.setFeeAmount(feeAmount);
        entity.setUpdatedAt(OffsetDateTime.now());

        repository.save(entity);
        log.info("ProcessSettlementSnapshotService - snapshot updated - processId={}", processId);
    }

    private WithdrawalTransactionEntity resolveDestinationWithdrawal(UUID processId) {
        List<WithdrawalTransactionEntity> withdrawals = withdrawalTransactionRepository
                .findByProcessIdOrderByCreatedAtAsc(processId);
        return withdrawals.get(1);
    }

    private BigDecimal resolveBtcUsd(FxRateSnapshot rates) {
        if (rates != null && rates.btcUsd() != null) {
            return rates.btcUsd();
        }
        return fxRateProvider.fetchAssetUsd("bitcoin").orElse(null);
    }

    private BigDecimal resolveBtcChainRate(String network, BigDecimal btcUsd) {
        if (btcUsd == null || btcUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (isBtcNetwork(network)) {
            return BigDecimal.ONE;
        }
        Optional<NetworkAssetResponse> asset = networkAssetService.findAsset(network);
        String priceId = asset.map(NetworkAssetResponse::priceId).orElse(null);
        if (priceId == null || priceId.isBlank()) {
            return null;
        }
        BigDecimal assetUsd = fxRateProvider.fetchAssetUsd(priceId).orElse(null);
        if (assetUsd == null || assetUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return btcUsd.divide(assetUsd, 12, RoundingMode.DOWN);
    }

    private boolean isBtcNetwork(String network) {
        if (network == null) {
            return true;
        }
        String normalized = network.trim().toUpperCase();
        return "BTC".equals(normalized)
                || "LBTC".equals(normalized)
                || "L-BTC".equals(normalized)
                || "LIQUID".equals(normalized);
    }

    private LightningPayload resolveLightningPayload(
            LightningInvoiceEntity invoice,
            String network,
            NetworkAssetResponse nativeAsset,
            BigDecimal btcUsd
    ) {
        if (invoice == null) {
            return new LightningPayload(null, null, null);
        }
        String txHash = null;
        BigDecimal amount = null;
        String payload = invoice.getSwapDecodedTransactionPayload();
        Map<String, Object> decoded = parseJson(payload);
        if (decoded != null) {
            txHash = asString(nested(decoded, "decodePayReq", "paymentHash"));
            amount = normalizeSatsToNative(
                    toBigDecimal(nested(decoded, "decodePayReq", "numSatoshis")),
                    network,
                    nativeAsset,
                    btcUsd
            );
        }
        BigDecimal fee = resolveLightningFee(invoice);
        fee = normalizeSatsToNative(fee, network, nativeAsset, btcUsd);
        return new LightningPayload(txHash, amount, fee);
    }

    private BigDecimal resolveLightningFee(LightningInvoiceEntity invoice) {
        BigDecimal fee = null;
        if (invoice.getSwapFeePercentage() != null && invoice.getSwapExpectedAmount() != null) {
            fee = BigDecimal.valueOf(invoice.getSwapExpectedAmount())
                    .multiply(BigDecimal.valueOf(invoice.getSwapFeePercentage()))
                    .divide(ONE_HUNDRED, 0, RoundingMode.DOWN);
        }
        if (invoice.getSwapMinerFees() != null) {
            BigDecimal miner = BigDecimal.valueOf(invoice.getSwapMinerFees());
            fee = fee == null ? miner : fee.add(miner);
        }
        return fee;
    }

    private ConversionPayload resolveConversionPayload(
            ConversionOrderEntity order,
            String network,
            NetworkAssetResponse nativeAsset,
            BigDecimal btcUsd
    ) {
        if (order == null) {
            return new ConversionPayload(null, null, null);
        }
        Map<String, Object> decoded = parseJson(order.getResponsePayload());
        if (decoded == null) {
            return new ConversionPayload(null, null, null);
        }

        Object txId = nested(decoded, "data", "to", "tx", "id");
        Object txAmount = nested(decoded, "data", "to", "tx", "amount");
        Object txFee = nested(decoded, "data", "to", "tx", "fee");

        BigDecimal amount = normalizeEthToNative(toBigDecimal(txAmount), network, nativeAsset, btcUsd);
        BigDecimal fee = normalizeEthToNative(toBigDecimal(txFee), network, nativeAsset, btcUsd);
        return new ConversionPayload(asString(txId), amount, fee);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal sum = null;
        for (BigDecimal value : values) {
            if (value == null) {
                continue;
            }
            sum = sum == null ? value : sum.add(value);
        }
        return sum;
    }

    private Map<String, Object> parseJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private Object nested(Map<String, Object> data, String... keys) {
        Object current = data;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(java.math.BigInteger value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value);
    }

    private BigDecimal normalizeWeiToNative(java.math.BigInteger value, NetworkAssetResponse nativeAsset) {
        if (value == null) {
            return null;
        }
        int decimals = nativeAsset == null || nativeAsset.decimals() == null ? 18 : nativeAsset.decimals();
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(decimals), 18, RoundingMode.DOWN);
    }

    private BigDecimal normalizeSatsToNative(
            BigDecimal sats,
            String network,
            NetworkAssetResponse nativeAsset,
            BigDecimal btcUsd
    ) {
        if (sats == null) {
            return null;
        }
        BigDecimal btc = sats.divide(SATS_PER_BTC, 12, RoundingMode.DOWN);
        if (isBtcNetwork(network)) {
            return btc;
        }
        BigDecimal assetUsd = resolveAssetUsd(nativeAsset);
        if (btcUsd == null || assetUsd == null || btcUsd.compareTo(BigDecimal.ZERO) <= 0 || assetUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal usd = btc.multiply(btcUsd).setScale(12, RoundingMode.DOWN);
        return usd.divide(assetUsd, 18, RoundingMode.DOWN);
    }

    private BigDecimal normalizeEthToNative(
            BigDecimal ethAmount,
            String network,
            NetworkAssetResponse nativeAsset,
            BigDecimal btcUsd
    ) {
        if (ethAmount == null) {
            return null;
        }
        String priceId = nativeAsset == null ? null : nativeAsset.priceId();
        if ("ethereum".equalsIgnoreCase(priceId) || isEthereumNetwork(network)) {
            return ethAmount;
        }
        BigDecimal ethUsd = fxRateProvider.fetchAssetUsd("ethereum").orElse(null);
        if (ethUsd == null || ethUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal usd = ethAmount.multiply(ethUsd).setScale(12, RoundingMode.DOWN);
        if (isBtcNetwork(network)) {
            if (btcUsd == null || btcUsd.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return usd.divide(btcUsd, 18, RoundingMode.DOWN);
        }
        BigDecimal assetUsd = resolveAssetUsd(nativeAsset);
        if (assetUsd == null || assetUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return usd.divide(assetUsd, 18, RoundingMode.DOWN);
    }

    private BigDecimal resolveAssetUsd(NetworkAssetResponse asset) {
        if (asset == null || asset.priceId() == null || asset.priceId().isBlank()) {
            return null;
        }
        return fxRateProvider.fetchAssetUsd(asset.priceId()).orElse(null);
    }

    private boolean isEthereumNetwork(String network) {
        if (network == null) {
            return false;
        }
        String normalized = network.trim().toLowerCase();
        return "ethereum".equals(normalized) || "eth".equals(normalized);
    }

    private record LightningPayload(String txHash, BigDecimal amount, BigDecimal fee) {
    }

    private record ConversionPayload(String txId, BigDecimal amount, BigDecimal fee) {
    }
}
