package com.walloop.engine.lightning;

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
@Table(name = "lightning_invoice", schema = "engine")
public class LightningInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String invoice;

    @Column
    private String boltzSwapId;

    @Column
    private String boltzLockupAddress;

    @Column
    private Long boltzExpectedAmount;

    @Column
    private String liquidTxId;

    @jakarta.persistence.Lob
    @Column
    private String boltzRequestPayload;

    @jakarta.persistence.Lob
    @Column
    private String boltzResponsePayload;

    @Column
    private String boltzStatus;

    @jakarta.persistence.Lob
    @Column
    private String boltzStatusPayload;

    private OffsetDateTime boltzPaidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LightningInvoiceStatus status;

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

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public String getBoltzSwapId() {
        return boltzSwapId;
    }

    public void setBoltzSwapId(String boltzSwapId) {
        this.boltzSwapId = boltzSwapId;
    }

    public String getBoltzLockupAddress() {
        return boltzLockupAddress;
    }

    public void setBoltzLockupAddress(String boltzLockupAddress) {
        this.boltzLockupAddress = boltzLockupAddress;
    }

    public Long getBoltzExpectedAmount() {
        return boltzExpectedAmount;
    }

    public void setBoltzExpectedAmount(Long boltzExpectedAmount) {
        this.boltzExpectedAmount = boltzExpectedAmount;
    }

    public String getLiquidTxId() {
        return liquidTxId;
    }

    public void setLiquidTxId(String liquidTxId) {
        this.liquidTxId = liquidTxId;
    }

    public String getBoltzRequestPayload() {
        return boltzRequestPayload;
    }

    public void setBoltzRequestPayload(String boltzRequestPayload) {
        this.boltzRequestPayload = boltzRequestPayload;
    }

    public String getBoltzResponsePayload() {
        return boltzResponsePayload;
    }

    public void setBoltzResponsePayload(String boltzResponsePayload) {
        this.boltzResponsePayload = boltzResponsePayload;
    }

    public String getBoltzStatus() {
        return boltzStatus;
    }

    public void setBoltzStatus(String boltzStatus) {
        this.boltzStatus = boltzStatus;
    }

    public String getBoltzStatusPayload() {
        return boltzStatusPayload;
    }

    public void setBoltzStatusPayload(String boltzStatusPayload) {
        this.boltzStatusPayload = boltzStatusPayload;
    }

    public OffsetDateTime getBoltzPaidAt() {
        return boltzPaidAt;
    }

    public void setBoltzPaidAt(OffsetDateTime boltzPaidAt) {
        this.boltzPaidAt = boltzPaidAt;
    }

    public LightningInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(LightningInvoiceStatus status) {
        this.status = status;
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
