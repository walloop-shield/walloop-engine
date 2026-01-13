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
@Table(name = "lightning_inbound_liquidity_requests", schema = "engine")
public class LightningInboundLiquidityRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LightningInboundLiquidityRequestStatus status;

    @Column(nullable = false)
    private long targetInboundSats;

    @Column(nullable = false)
    private long currentInboundSats;

    @Column(nullable = false)
    private long requestedSats;

    @Column
    private String externalId;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column
    private String errorMessage;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LightningInboundLiquidityRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LightningInboundLiquidityRequestStatus status) {
        this.status = status;
    }

    public long getTargetInboundSats() {
        return targetInboundSats;
    }

    public void setTargetInboundSats(long targetInboundSats) {
        this.targetInboundSats = targetInboundSats;
    }

    public long getCurrentInboundSats() {
        return currentInboundSats;
    }

    public void setCurrentInboundSats(long currentInboundSats) {
        this.currentInboundSats = currentInboundSats;
    }

    public long getRequestedSats() {
        return requestedSats;
    }

    public void setRequestedSats(long requestedSats) {
        this.requestedSats = requestedSats;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
