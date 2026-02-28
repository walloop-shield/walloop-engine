package io.walloop.engine.lightning;

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
@Table(name = "lightning_outbound_liquidity_requests", schema = "engine")
public class LightningOutboundLiquidityRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String targetNodePubkey;

    @Column(nullable = false)
    private long targetChannelId;

    @Column(nullable = false)
    private long targetChannelCapacitySats;

    @Column(nullable = false)
    private long targetChannelLocalBalanceSats;

    @Column(nullable = false)
    private long targetChannelRemoteBalanceSats;

    @Column(nullable = false)
    private long targetChannelLocalReserveSats;

    @Column(nullable = false)
    private long targetChannelCommitFeeSats;

    @Column(nullable = false)
    private long targetChannelSpendableSats;

    @Column(nullable = false)
    private long requestedSats;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String invoice;

    @Column(nullable = false)
    private String paymentHash;

    @Column
    private String invoiceMemo;

    @Column(nullable = false)
    private long invoiceExpirySeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LightningOutboundLiquidityRequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int pollAttempts;

    @Column
    private OffsetDateTime lastPolledAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime paidAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTargetNodePubkey() {
        return targetNodePubkey;
    }

    public void setTargetNodePubkey(String targetNodePubkey) {
        this.targetNodePubkey = targetNodePubkey;
    }

    public long getTargetChannelId() {
        return targetChannelId;
    }

    public void setTargetChannelId(long targetChannelId) {
        this.targetChannelId = targetChannelId;
    }

    public long getTargetChannelCapacitySats() {
        return targetChannelCapacitySats;
    }

    public void setTargetChannelCapacitySats(long targetChannelCapacitySats) {
        this.targetChannelCapacitySats = targetChannelCapacitySats;
    }

    public long getTargetChannelLocalBalanceSats() {
        return targetChannelLocalBalanceSats;
    }

    public void setTargetChannelLocalBalanceSats(long targetChannelLocalBalanceSats) {
        this.targetChannelLocalBalanceSats = targetChannelLocalBalanceSats;
    }

    public long getTargetChannelRemoteBalanceSats() {
        return targetChannelRemoteBalanceSats;
    }

    public void setTargetChannelRemoteBalanceSats(long targetChannelRemoteBalanceSats) {
        this.targetChannelRemoteBalanceSats = targetChannelRemoteBalanceSats;
    }

    public long getTargetChannelLocalReserveSats() {
        return targetChannelLocalReserveSats;
    }

    public void setTargetChannelLocalReserveSats(long targetChannelLocalReserveSats) {
        this.targetChannelLocalReserveSats = targetChannelLocalReserveSats;
    }

    public long getTargetChannelCommitFeeSats() {
        return targetChannelCommitFeeSats;
    }

    public void setTargetChannelCommitFeeSats(long targetChannelCommitFeeSats) {
        this.targetChannelCommitFeeSats = targetChannelCommitFeeSats;
    }

    public long getTargetChannelSpendableSats() {
        return targetChannelSpendableSats;
    }

    public void setTargetChannelSpendableSats(long targetChannelSpendableSats) {
        this.targetChannelSpendableSats = targetChannelSpendableSats;
    }

    public long getRequestedSats() {
        return requestedSats;
    }

    public void setRequestedSats(long requestedSats) {
        this.requestedSats = requestedSats;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public String getPaymentHash() {
        return paymentHash;
    }

    public void setPaymentHash(String paymentHash) {
        this.paymentHash = paymentHash;
    }

    public String getInvoiceMemo() {
        return invoiceMemo;
    }

    public void setInvoiceMemo(String invoiceMemo) {
        this.invoiceMemo = invoiceMemo;
    }

    public long getInvoiceExpirySeconds() {
        return invoiceExpirySeconds;
    }

    public void setInvoiceExpirySeconds(long invoiceExpirySeconds) {
        this.invoiceExpirySeconds = invoiceExpirySeconds;
    }

    public LightningOutboundLiquidityRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LightningOutboundLiquidityRequestStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getPollAttempts() {
        return pollAttempts;
    }

    public void setPollAttempts(int pollAttempts) {
        this.pollAttempts = pollAttempts;
    }

    public OffsetDateTime getLastPolledAt() {
        return lastPolledAt;
    }

    public void setLastPolledAt(OffsetDateTime lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
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

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }
}

