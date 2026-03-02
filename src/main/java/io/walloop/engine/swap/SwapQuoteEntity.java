package io.walloop.engine.swap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "swap_quote", schema = "engine")
public class SwapQuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwapPartner partner;

    @Column(nullable = false)
    private String fromCoin;

    @Column
    private String fromNetwork;

    @Column(nullable = false)
    private String toCoin;

    @Column
    private String toNetwork;

    @Column
    private String amount;

    @Column
    private String lastBalance;

    @Column
    private String min;

    @Column
    private String max;

    @Column
    private String rate;

    @Column
    private String depositCoin;

    @Column
    private String settleCoin;

    @Column
    private String depositNetwork;

    @Column
    private String settleNetwork;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public SwapPartner getPartner() {
        return partner;
    }

    public void setPartner(SwapPartner partner) {
        this.partner = partner;
    }

    public String getFromCoin() {
        return fromCoin;
    }

    public void setFromCoin(String fromCoin) {
        this.fromCoin = fromCoin;
    }

    public String getFromNetwork() {
        return fromNetwork;
    }

    public void setFromNetwork(String fromNetwork) {
        this.fromNetwork = fromNetwork;
    }

    public String getToCoin() {
        return toCoin;
    }

    public void setToCoin(String toCoin) {
        this.toCoin = toCoin;
    }

    public String getToNetwork() {
        return toNetwork;
    }

    public void setToNetwork(String toNetwork) {
        this.toNetwork = toNetwork;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getLastBalance() {
        return lastBalance;
    }

    public void setLastBalance(String lastBalance) {
        this.lastBalance = lastBalance;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getDepositCoin() {
        return depositCoin;
    }

    public void setDepositCoin(String depositCoin) {
        this.depositCoin = depositCoin;
    }

    public String getSettleCoin() {
        return settleCoin;
    }

    public void setSettleCoin(String settleCoin) {
        this.settleCoin = settleCoin;
    }

    public String getDepositNetwork() {
        return depositNetwork;
    }

    public void setDepositNetwork(String depositNetwork) {
        this.depositNetwork = depositNetwork;
    }

    public String getSettleNetwork() {
        return settleNetwork;
    }

    public void setSettleNetwork(String settleNetwork) {
        this.settleNetwork = settleNetwork;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

