package com.walloop.engine.onboarding;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, String> {
    Optional<LoginSessionEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
