package com.walloop.engine.deposit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositMonitorRepository extends JpaRepository<DepositMonitorEntity, UUID> {
}
