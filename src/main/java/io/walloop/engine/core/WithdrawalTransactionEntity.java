package io.walloop.engine.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "withdrawal_transactions", schema = "core")
public class WithdrawalTransactionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false, length = 20)
    private String chain;

    @Column(name = "fee", nullable = false, precision = 78, scale = 0)
    private BigInteger feeBaseUnit;

    @Column(name = "amount", nullable = false, precision = 78, scale = 0)
    private BigInteger amountBaseUnit;

    @Column(nullable = false, length = 32)
    private String destination;

    @Column(nullable = false)
    private boolean commissionFeeApplied;

    @Column(length = 128)
    private String txHash;

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

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public BigInteger getFeeBaseUnit() {
        return feeBaseUnit;
    }

    public void setFeeBaseUnit(BigInteger feeBaseUnit) {
        this.feeBaseUnit = feeBaseUnit;
    }

    public BigInteger getAmountBaseUnit() {
        return amountBaseUnit;
    }

    public void setAmountBaseUnit(BigInteger amountBaseUnit) {
        this.amountBaseUnit = amountBaseUnit;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isCommissionFeeApplied() {
        return commissionFeeApplied;
    }

    public void setCommissionFeeApplied(boolean commissionFeeApplied) {
        this.commissionFeeApplied = commissionFeeApplied;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

