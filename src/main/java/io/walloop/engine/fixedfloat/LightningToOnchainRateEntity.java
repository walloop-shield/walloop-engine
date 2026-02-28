package io.walloop.engine.fixedfloat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lightning_to_onchain_rates", schema = "engine")
public class LightningToOnchainRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fromAsset;

    @Column(nullable = false)
    private String toAsset;

    @Column
    private String network;

    @Column
    private java.math.BigDecimal inValue;

    @Column
    private java.math.BigDecimal outValue;

    @Column
    private java.math.BigDecimal amount;

    @Column
    private java.math.BigDecimal toFeeValue;

    @Column
    private java.math.BigDecimal minValue;

    @Column
    private java.math.BigDecimal maxValue;

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

    public String getFromAsset() {
        return fromAsset;
    }

    public void setFromAsset(String fromAsset) {
        this.fromAsset = fromAsset;
    }

    public String getToAsset() {
        return toAsset;
    }

    public void setToAsset(String toAsset) {
        this.toAsset = toAsset;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public java.math.BigDecimal getInValue() {
        return inValue;
    }

    public void setInValue(java.math.BigDecimal inValue) {
        this.inValue = inValue;
    }

    public java.math.BigDecimal getOutValue() {
        return outValue;
    }

    public void setOutValue(java.math.BigDecimal outValue) {
        this.outValue = outValue;
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public java.math.BigDecimal getToFeeValue() {
        return toFeeValue;
    }

    public void setToFeeValue(java.math.BigDecimal toFeeValue) {
        this.toFeeValue = toFeeValue;
    }

    public java.math.BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(java.math.BigDecimal minValue) {
        this.minValue = minValue;
    }

    public java.math.BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(java.math.BigDecimal maxValue) {
        this.maxValue = maxValue;
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

