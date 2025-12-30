package com.walloop.engine;

import org.junit.jupiter.api.Test;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class WalloopEngineApplicationTests {

    @MockBean
    private SynchronousLndAPI synchronousLndAPI;

    @Test
    void contextLoads() {
    }
}
