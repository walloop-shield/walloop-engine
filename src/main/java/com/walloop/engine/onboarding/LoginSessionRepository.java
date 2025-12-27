package com.walloop.engine.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, String> {
    java.util.Optional<LoginSessionEntity> findBySessionToken(String sessionToken);
}
