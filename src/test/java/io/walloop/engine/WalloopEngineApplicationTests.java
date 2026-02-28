package io.walloop.engine;

import org.junit.jupiter.api.Test;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.walloop.engine.fee.FxRateProvider;
import io.walloop.engine.lightning.LspLiquidityService;

@SpringBootTest(properties = "walloop.wallet.base-url=http://localhost:8080")
class WalloopEngineApplicationTests {

    @MockBean
    private SynchronousLndAPI synchronousLndAPI;

    @MockBean
    private LspLiquidityService lspLiquidityService;

    @MockBean
    private FxRateProvider fxRateProvider;

    @Test
    void contextLoads() {
    }
}

