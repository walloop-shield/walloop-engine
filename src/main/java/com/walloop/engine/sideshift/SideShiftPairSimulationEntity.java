package com.walloop.engine.sideshift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sideshift_pair_simulation", schema = "engine")
public class SideShiftPairSimulationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false)
    private String fromCoin;

    @Column
    private String fromNetwork;

    @Column(nullable = false)
    private String toCoin;

    @Column
    private String toNetwork;

    @Column(name = "amount")
    private String amount;

    @Column(name = "last_balance")
    private String lastBalance;

    @Column(name = "min")
    private String min;

    @Column(name = "max")
    private String max;

    @Column(name = "rate")
    private String rate;

    @Column(name = "deposit_coin")
    private String depositCoin;

    @Column(name = "settle_coin")
    private String settleCoin;

    @Column(name = "deposit_network")
    private String depositNetwork;

    @Column(name = "settle_network")
    private String settleNetwork;

    @Lob
    @Column
    private String requestPayload;

    @Lob
    @Column
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
