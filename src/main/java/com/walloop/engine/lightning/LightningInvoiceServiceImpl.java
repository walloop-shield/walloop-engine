package com.walloop.engine.lightning;

import com.walloop.engine.fee.FxRateProvider;
import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.liquid.service.LiquidRpcService;
import com.walloop.engine.swap.SwapQuoteEntity;
import com.walloop.engine.swap.SwapQuoteRepository;
import com.walloop.engine.lightning.swap.LightningSwapFeeProvider;
import com.walloop.engine.lightning.swap.LightningSwapFeeQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.AddInvoiceResponse;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LightningInvoiceServiceImpl implements LightningInvoiceService {

    private static final int LIQUID_FEE_CONF_TARGET = 6;
    private static final int LIQUID_TX_VBYTES = 250;
    private static final long LIQUID_FEE_BUFFER_SATS = 200;

    private final LightningInvoiceRepository repository;
    private final SynchronousLndAPI lndApi;
    private final LiquidWalletRepository liquidWalletRepository;
    private final LiquidRpcService liquidRpcService;
    private final SwapQuoteRepository swapQuoteRepository;
    private final FxRateProvider fxRateProvider;
    private final LightningSwapFeeProvider lightningSwapFeeProvider;

    @Value("${walloop.lightning.invoice-expiry-seconds:7200}")
    private long invoiceExpirySeconds;

    @Override
    public String createOrGetInvoice(UUID processId, UUID ownerId) {
        return repository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .map(existing -> resolveInvoice(processId, ownerId, existing))
                .orElseGet(() -> createInvoice(processId, ownerId));
    }

    @Override
    public long resolveInvoiceAmountSats(UUID processId, UUID ownerId) {
        BalanceAmount amount = resolveBalanceAmount(processId, ownerId);
        return amount.balanceSats();
    }

    private String resolveInvoice(UUID processId, UUID ownerId, LightningInvoiceEntity existing) {
        if (existing.getStatus() != LightningInvoiceStatus.CREATED) {
            return existing.getInvoice();
        }

        if (isExpired(existing)) {
            return createInvoice(processId, ownerId);
        }

        BalanceSnapshot snapshot = buildBalanceSnapshot(processId, ownerId);
        Long existingMsats = existing.getBalanceMsats();
        if (existingMsats == null || !existingMsats.equals(snapshot.balanceMsats())) {
            return createInvoice(processId, ownerId);
        }

        return existing.getInvoice();
    }

    private boolean isExpired(LightningInvoiceEntity existing) {
        OffsetDateTime createdAt = existing.getCreatedAt();
        if (createdAt == null) {
            return true;
        }
        return createdAt.plusSeconds(invoiceExpirySeconds).isBefore(OffsetDateTime.now());
    }

    private String createInvoice(UUID processId, UUID ownerId) {
        BalanceSnapshot snapshot = buildBalanceSnapshot(processId, ownerId);
        String invoice = createInvoiceViaLnd(processId, snapshot.balanceMsats());
        if (invoice == null || invoice.isBlank()) {
            throw new IllegalStateException("Lightning invoice not returned by LND");
        }

        LightningInvoiceEntity entity = new LightningInvoiceEntity();
        entity.setProcessId(processId);
        entity.setOwnerId(ownerId);
        entity.setInvoice(invoice);
        entity.setBalanceBtc(snapshot.balanceBtc());
        entity.setBalanceSats(snapshot.balanceSats());
        entity.setBalanceMsats(snapshot.balanceMsats());
        entity.setBalanceUsdt(snapshot.balanceUsdt());
        if (snapshot.feeQuote() != null) {
            entity.setSwapFeePercentage(snapshot.feeQuote().percentage());
            entity.setSwapMinerFees(snapshot.feeQuote().minerFees());
            entity.setSwapPairHash(snapshot.feeQuote().pairHash());
        }
        entity.setLiquidFeeSats(snapshot.liquidFeeSats());
        entity.setLiquidFeeConfTarget(LIQUID_FEE_CONF_TARGET);
        entity.setLiquidFeeVbytes(LIQUID_TX_VBYTES);
        entity.setStatus(LightningInvoiceStatus.CREATED);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
        return invoice;
    }

    private String createInvoiceViaLnd(UUID processId, long amountMsats) {
        try {
            Invoice invoice = new Invoice();
            invoice.setMemo("walloop:" + processId);
            invoice.setValueMsat(amountMsats);
            invoice.setExpiry(invoiceExpirySeconds);
            AddInvoiceResponse response = lndApi.addInvoice(invoice);
            return response.getPaymentRequest();
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to create LND invoice for processId=" + processId, e);
        }
    }

    private BalanceSnapshot buildBalanceSnapshot(UUID processId, UUID ownerId) {
        BalanceAmount amount = resolveBalanceAmount(processId, ownerId);
        BigDecimal balanceBtc = new BigDecimal(amount.balanceBtc());
        BigDecimal balanceUsdt = resolveUsdtValue(processId, balanceBtc);
        return new BalanceSnapshot(
                amount.balanceBtc(),
                amount.balanceSats(),
                amount.balanceMsats(),
                balanceUsdt.stripTrailingZeros().toPlainString(),
                amount.feeQuote(),
                amount.liquidFeeSats()
        );
    }

    private BalanceAmount resolveBalanceAmount(UUID processId, UUID ownerId) {
        LiquidWalletEntity wallet = liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                .orElseThrow(() -> new IllegalStateException("Liquid wallet not found"));

        BigDecimal balanceBtc = liquidRpcService.getReceivedByAddress(wallet.getAddress());
        if (balanceBtc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Liquid wallet balance is empty");
        }

        long balanceSats = balanceBtc.movePointRight(8)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
        long feeReserveSats = liquidRpcService.estimateFeeSats(LIQUID_FEE_CONF_TARGET, LIQUID_TX_VBYTES)
                + LIQUID_FEE_BUFFER_SATS;
        if (balanceSats <= feeReserveSats) {
            throw new IllegalStateException("Liquid balance below estimated transaction fee");
        }
        long spendableSats = balanceSats - feeReserveSats;
        LightningSwapFeeQuote feeQuote = lightningSwapFeeProvider.quoteInvoice(spendableSats);
        long invoiceSats = feeQuote.invoiceSats();
        long balanceMsats = BigDecimal.valueOf(invoiceSats)
                .movePointRight(3)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
        String balanceBtcValue = BigDecimal.valueOf(invoiceSats)
                .movePointLeft(8)
                .stripTrailingZeros()
                .toPlainString();
        return new BalanceAmount(
                balanceBtcValue,
                invoiceSats,
                balanceMsats,
                feeQuote,
                feeReserveSats
        );
    }

    private BigDecimal resolveUsdtValue(UUID processId, BigDecimal balanceBtc) {
        Optional<SwapQuoteEntity> simulation = swapQuoteRepository
                .findFirstByProcessIdOrderByCreatedAtDesc(processId);
        if (simulation.isPresent()) {
            BigDecimal usdtFromPair = resolveUsdtFromPair(balanceBtc, simulation.get());
            if (usdtFromPair != null) {
                return usdtFromPair;
            }
        }

        return fxRateProvider.fetchAssetUsd("bitcoin")
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) > 0)
                .map(rate -> balanceBtc.multiply(rate).setScale(8, RoundingMode.DOWN))
                .orElseThrow(() -> new IllegalStateException("USDT rate not available for processId=" + processId));
    }

    private BigDecimal resolveUsdtFromPair(BigDecimal balanceBtc, SwapQuoteEntity simulation) {
        String rateValue = simulation.getRate();
        if (rateValue == null || rateValue.isBlank()) {
            return null;
        }
        BigDecimal rate = new BigDecimal(rateValue);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String fromCoin = simulation.getFromCoin();
        String toCoin = simulation.getToCoin();
        if ("usdt".equalsIgnoreCase(fromCoin) && isBtcSymbol(toCoin)) {
            return balanceBtc.divide(rate, 8, RoundingMode.DOWN);
        }
        if (isBtcSymbol(fromCoin) && "usdt".equalsIgnoreCase(toCoin)) {
            return balanceBtc.multiply(rate).setScale(8, RoundingMode.DOWN);
        }
        return null;
    }

    private boolean isBtcSymbol(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "btc".equals(normalized)
                || "l-btc".equals(normalized)
                || "lbtc".equals(normalized)
                || "liquid".equals(normalized);
    }

    private record BalanceSnapshot(
            String balanceBtc,
            long balanceSats,
            long balanceMsats,
            String balanceUsdt,
            LightningSwapFeeQuote feeQuote,
            long liquidFeeSats
    ) {
    }

    private record BalanceAmount(
            String balanceBtc,
            long balanceSats,
            long balanceMsats,
            LightningSwapFeeQuote feeQuote,
            long liquidFeeSats
    ) {
    }
}
