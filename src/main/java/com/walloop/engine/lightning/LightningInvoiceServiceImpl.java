package com.walloop.engine.lightning;

import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.liquid.service.LiquidRpcService;
import com.walloop.engine.sideshift.SideShiftPairSimulationEntity;
import com.walloop.engine.sideshift.SideShiftPairSimulationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

    private final LightningInvoiceRepository repository;
    private final SynchronousLndAPI lndApi;
    private final LiquidWalletRepository liquidWalletRepository;
    private final LiquidRpcService liquidRpcService;
    private final SideShiftPairSimulationRepository pairSimulationRepository;

    @Value("${walloop.lightning.invoice-expiry-seconds:7200}")
    private long invoiceExpirySeconds;

    @Override
    public String createOrGetInvoice(UUID processId, UUID ownerId) {
        return repository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .map(LightningInvoiceEntity::getInvoice)
                .orElseGet(() -> createInvoice(processId, ownerId));
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
        LiquidWalletEntity wallet = liquidWalletRepository.findFirstByTransactionIdAndOwnerId(processId, ownerId)
                .orElseThrow(() -> new IllegalStateException("Liquid wallet not found for processId=" + processId));

        BigDecimal balanceBtc = liquidRpcService.getReceivedByAddress(wallet.getAddress());
        if (balanceBtc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Liquid wallet balance is empty for processId=" + processId);
        }

        long balanceSats = balanceBtc.movePointRight(8)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
        long balanceMsats = balanceBtc.movePointRight(11)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();

        BigDecimal balanceUsdt = resolveUsdtValue(processId, balanceBtc);
        return new BalanceSnapshot(
                balanceBtc.stripTrailingZeros().toPlainString(),
                balanceSats,
                balanceMsats,
                balanceUsdt.stripTrailingZeros().toPlainString()
        );
    }

    private BigDecimal resolveUsdtValue(UUID processId, BigDecimal balanceBtc) {
        SideShiftPairSimulationEntity simulation = pairSimulationRepository
                .findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElseThrow(() -> new IllegalStateException("SideShift pair simulation not found for processId=" + processId));

        String rateValue = simulation.getRate();
        if (rateValue == null || rateValue.isBlank()) {
            throw new IllegalStateException("SideShift pair rate not available for processId=" + processId);
        }

        BigDecimal rate = new BigDecimal(rateValue);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("SideShift pair rate invalid for processId=" + processId);
        }
        String fromCoin = simulation.getFromCoin();
        String toCoin = simulation.getToCoin();
        if ("usdt".equalsIgnoreCase(fromCoin) && "btc".equalsIgnoreCase(toCoin)) {
            return balanceBtc.divide(rate, 8, RoundingMode.DOWN);
        }
        if ("btc".equalsIgnoreCase(fromCoin) && "usdt".equalsIgnoreCase(toCoin)) {
            return balanceBtc.multiply(rate).setScale(8, RoundingMode.DOWN);
        }

        throw new IllegalStateException("SideShift pair does not cover BTC/USDT for processId=" + processId);
    }

    private record BalanceSnapshot(String balanceBtc, long balanceSats, long balanceMsats, String balanceUsdt) {
    }
}
