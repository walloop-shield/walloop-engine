package com.walloop.engine.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigInteger;
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
}
