package io.walloop.engine.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "deposit_watch", schema = "core")
public class DepositWatchEntity {

    @Id
    @Column(name = "process_id")
    private UUID processId;

    @Column(name = "network")
    private String network;

    @Column(name = "lastBalance")
    private String lastBalance;

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getLastBalance() {
        return lastBalance;
    }

    public void setLastBalance(String lastBalance) {
        this.lastBalance = lastBalance;
    }
}

