package io.walloop.engine.fixedfloat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LightningToOnchainRateRepository extends JpaRepository<LightningToOnchainRateEntity, UUID> {

    @Query("""
            select r from LightningToOnchainRateEntity r
            where lower(r.fromAsset) = lower(:fromAsset)
              and lower(r.network) = lower(:network)
            order by r.updatedAt desc
            """)
    Optional<LightningToOnchainRateEntity> findLatestByFromAssetAndNetwork(
            @Param("fromAsset") String fromAsset,
            @Param("network") String network
    );
}

