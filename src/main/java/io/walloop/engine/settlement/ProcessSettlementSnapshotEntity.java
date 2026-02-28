package io.walloop.engine.settlement;

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
@Table(name = "process_settlement_snapshot", schema = "engine")
public class ProcessSettlementSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column
    private String blockchain;

    @Column(precision = 30, scale = 18)
    private BigDecimal feePercent;

    @Column(precision = 78, scale = 18)
    private BigDecimal feeAmount;

    @Column(precision = 30, scale = 12)
    private BigDecimal rateIdxDollarReal;

    @Column(precision = 30, scale = 12)
    private BigDecimal rateIdxBitcoinDollar;

    @Column(precision = 30, scale = 12)
    private BigDecimal rateIdxBitcoinChain;

    @Column(precision = 78, scale = 0)
    private BigDecimal initialAmountDetected;

    @Column
    private String liquidTxUrl;

    @Column(precision = 78, scale = 18)
    private BigDecimal liquidAmount;

    @Column(precision = 78, scale = 18)
    private BigDecimal liquidFee;

    @Column
    private String lightningTxUrl;

    @Column(precision = 78, scale = 18)
    private BigDecimal lightningAmount;

    @Column(precision = 78, scale = 18)
    private BigDecimal lightningFee;

    @Column
    private String conversionTxUrl;

    @Column(precision = 78, scale = 18)
    private BigDecimal conversionAmount;

    @Column(precision = 78, scale = 18)
    private BigDecimal conversionFee;

    @Column
    private String destinationTxUrl;

    @Column(precision = 78, scale = 18)
    private BigDecimal destinationAmount;

    @Column(precision = 78, scale = 18)
    private BigDecimal destinationFee;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

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

    public String getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(String blockchain) {
        this.blockchain = blockchain;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(BigDecimal feePercent) {
        this.feePercent = feePercent;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getRateIdxDollarReal() {
        return rateIdxDollarReal;
    }

    public void setRateIdxDollarReal(BigDecimal rateIdxDollarReal) {
        this.rateIdxDollarReal = rateIdxDollarReal;
    }

    public BigDecimal getRateIdxBitcoinDollar() {
        return rateIdxBitcoinDollar;
    }

    public void setRateIdxBitcoinDollar(BigDecimal rateIdxBitcoinDollar) {
        this.rateIdxBitcoinDollar = rateIdxBitcoinDollar;
    }

    public BigDecimal getRateIdxBitcoinChain() {
        return rateIdxBitcoinChain;
    }

    public void setRateIdxBitcoinChain(BigDecimal rateIdxBitcoinChain) {
        this.rateIdxBitcoinChain = rateIdxBitcoinChain;
    }

    public BigDecimal getInitialAmountDetected() {
        return initialAmountDetected;
    }

    public void setInitialAmountDetected(BigDecimal initialAmountDetected) {
        this.initialAmountDetected = initialAmountDetected;
    }

    public String getLiquidTxUrl() {
        return liquidTxUrl;
    }

    public void setLiquidTxUrl(String liquidTxUrl) {
        this.liquidTxUrl = liquidTxUrl;
    }

    public BigDecimal getLiquidAmount() {
        return liquidAmount;
    }

    public void setLiquidAmount(BigDecimal liquidAmount) {
        this.liquidAmount = liquidAmount;
    }

    public BigDecimal getLiquidFee() {
        return liquidFee;
    }

    public void setLiquidFee(BigDecimal liquidFee) {
        this.liquidFee = liquidFee;
    }

    public String getLightningTxUrl() {
        return lightningTxUrl;
    }

    public void setLightningTxUrl(String lightningTxUrl) {
        this.lightningTxUrl = lightningTxUrl;
    }

    public BigDecimal getLightningAmount() {
        return lightningAmount;
    }

    public void setLightningAmount(BigDecimal lightningAmount) {
        this.lightningAmount = lightningAmount;
    }

    public BigDecimal getLightningFee() {
        return lightningFee;
    }

    public void setLightningFee(BigDecimal lightningFee) {
        this.lightningFee = lightningFee;
    }

    public String getConversionTxUrl() {
        return conversionTxUrl;
    }

    public void setConversionTxUrl(String conversionTxUrl) {
        this.conversionTxUrl = conversionTxUrl;
    }

    public BigDecimal getConversionAmount() {
        return conversionAmount;
    }

    public void setConversionAmount(BigDecimal conversionAmount) {
        this.conversionAmount = conversionAmount;
    }

    public BigDecimal getConversionFee() {
        return conversionFee;
    }

    public void setConversionFee(BigDecimal conversionFee) {
        this.conversionFee = conversionFee;
    }

    public String getDestinationTxUrl() {
        return destinationTxUrl;
    }

    public void setDestinationTxUrl(String destinationTxUrl) {
        this.destinationTxUrl = destinationTxUrl;
    }

    public BigDecimal getDestinationAmount() {
        return destinationAmount;
    }

    public void setDestinationAmount(BigDecimal destinationAmount) {
        this.destinationAmount = destinationAmount;
    }

    public BigDecimal getDestinationFee() {
        return destinationFee;
    }

    public void setDestinationFee(BigDecimal destinationFee) {
        this.destinationFee = destinationFee;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

