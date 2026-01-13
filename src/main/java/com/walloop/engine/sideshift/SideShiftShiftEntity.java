package com.walloop.engine.sideshift;

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
@Table(name = "sideshift_shift", schema = "engine")
public class SideShiftShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column
    private String shiftId;

    @Column
    private String depositAddress;

    @Column
    private String depositNetwork;

    @Column
    private String depositTxId;

    @Column
    private String userIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SideShiftShiftStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String responsePayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    private OffsetDateTime withdrawRequestedAt;

    private OffsetDateTime withdrawCompletedAt;

    private OffsetDateTime settledAt;

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

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getDepositAddress() {
        return depositAddress;
    }

    public void setDepositAddress(String depositAddress) {
        this.depositAddress = depositAddress;
    }

    public String getDepositNetwork() {
        return depositNetwork;
    }

    public void setDepositNetwork(String depositNetwork) {
        this.depositNetwork = depositNetwork;
    }

    public String getDepositTxId() {
        return depositTxId;
    }

    public void setDepositTxId(String depositTxId) {
        this.depositTxId = depositTxId;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public SideShiftShiftStatus getStatus() {
        return status;
    }

    public void setStatus(SideShiftShiftStatus status) {
        this.status = status;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getWithdrawRequestedAt() {
        return withdrawRequestedAt;
    }

    public void setWithdrawRequestedAt(OffsetDateTime withdrawRequestedAt) {
        this.withdrawRequestedAt = withdrawRequestedAt;
    }

    public OffsetDateTime getWithdrawCompletedAt() {
        return withdrawCompletedAt;
    }

    public void setWithdrawCompletedAt(OffsetDateTime withdrawCompletedAt) {
        this.withdrawCompletedAt = withdrawCompletedAt;
    }

    public OffsetDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(OffsetDateTime settledAt) {
        this.settledAt = settledAt;
    }
}
