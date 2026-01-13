package com.walloop.engine.fee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_calculation", schema = "engine")
public class FeeCalculationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private Long amountSats;

    @Column
    private BigDecimal amountBtc;

    @Column
    private BigDecimal amountUsd;

    @Column
    private BigDecimal amountBrl;

    @Column(nullable = false)
    private BigDecimal feePercent;

    @Column(nullable = false)
    private Long feeSats;

    @Column(nullable = false)
    private Long onchainFeeSats;

    @Column(nullable = false)
    private Long totalFeeSats;

    @Column
    private BigDecimal feeBtc;

    @Column
    private BigDecimal feeUsd;

    @Column
    private BigDecimal feeBrl;

    @Column
    private BigDecimal totalFeeBtc;

    @Column
    private BigDecimal totalFeeUsd;

    @Column
    private BigDecimal totalFeeBrl;

    @Column(columnDefinition = "TEXT")
    private String payload;

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

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Long getAmountSats() {
        return amountSats;
    }

    public void setAmountSats(Long amountSats) {
        this.amountSats = amountSats;
    }

    public BigDecimal getAmountBtc() {
        return amountBtc;
    }

    public void setAmountBtc(BigDecimal amountBtc) {
        this.amountBtc = amountBtc;
    }

    public BigDecimal getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(BigDecimal amountUsd) {
        this.amountUsd = amountUsd;
    }

    public BigDecimal getAmountBrl() {
        return amountBrl;
    }

    public void setAmountBrl(BigDecimal amountBrl) {
        this.amountBrl = amountBrl;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(BigDecimal feePercent) {
        this.feePercent = feePercent;
    }

    public Long getFeeSats() {
        return feeSats;
    }

    public void setFeeSats(Long feeSats) {
        this.feeSats = feeSats;
    }

    public Long getOnchainFeeSats() {
        return onchainFeeSats;
    }

    public void setOnchainFeeSats(Long onchainFeeSats) {
        this.onchainFeeSats = onchainFeeSats;
    }

    public Long getTotalFeeSats() {
        return totalFeeSats;
    }

    public void setTotalFeeSats(Long totalFeeSats) {
        this.totalFeeSats = totalFeeSats;
    }

    public BigDecimal getFeeBtc() {
        return feeBtc;
    }

    public void setFeeBtc(BigDecimal feeBtc) {
        this.feeBtc = feeBtc;
    }

    public BigDecimal getFeeUsd() {
        return feeUsd;
    }

    public void setFeeUsd(BigDecimal feeUsd) {
        this.feeUsd = feeUsd;
    }

    public BigDecimal getFeeBrl() {
        return feeBrl;
    }

    public void setFeeBrl(BigDecimal feeBrl) {
        this.feeBrl = feeBrl;
    }

    public BigDecimal getTotalFeeBtc() {
        return totalFeeBtc;
    }

    public void setTotalFeeBtc(BigDecimal totalFeeBtc) {
        this.totalFeeBtc = totalFeeBtc;
    }

    public BigDecimal getTotalFeeUsd() {
        return totalFeeUsd;
    }

    public void setTotalFeeUsd(BigDecimal totalFeeUsd) {
        this.totalFeeUsd = totalFeeUsd;
    }

    public BigDecimal getTotalFeeBrl() {
        return totalFeeBrl;
    }

    public void setTotalFeeBrl(BigDecimal totalFeeBrl) {
        this.totalFeeBrl = totalFeeBrl;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
