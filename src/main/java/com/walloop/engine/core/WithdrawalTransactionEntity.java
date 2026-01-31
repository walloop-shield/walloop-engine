package com.walloop.engine.core;

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

    @Column(nullable = false, precision = 78, scale = 0)
    private BigInteger feeWei;

    @Column(nullable = false, precision = 78, scale = 0)
    private BigInteger amountWei;

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

    public BigInteger getFeeWei() {
        return feeWei;
    }

    public void setFeeWei(BigInteger feeWei) {
        this.feeWei = feeWei;
    }

    public BigInteger getAmountWei() {
        return amountWei;
    }

    public void setAmountWei(BigInteger amountWei) {
        this.amountWei = amountWei;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
