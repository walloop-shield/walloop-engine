package io.walloop.engine.fee;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FxRateProvider.class)
public class NoopFxRateProvider implements FxRateProvider {

    @Override
    public Optional<FxRateSnapshot> fetch() {
        return Optional.empty();
    }
}

