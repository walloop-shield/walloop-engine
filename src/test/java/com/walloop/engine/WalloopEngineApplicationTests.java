package com.walloop.engine;

import org.junit.jupiter.api.Test;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "walloop.wallet.base-url=http://localhost:8080")
class WalloopEngineApplicationTests {

    @MockBean
    private SynchronousLndAPI synchronousLndAPI;

    @Test
    void contextLoads() {
    }
}
