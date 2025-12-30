package com.walloop.engine.sideshift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.onboarding.LoginSessionEntity;
import com.walloop.engine.onboarding.LoginSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SideShiftSwapServiceTest {

    @Test
    void usesUserIpFromLoginSession() {
        SideShiftClient client = Mockito.mock(SideShiftClient.class);
        SideShiftShiftRepository shiftRepository = Mockito.mock(SideShiftShiftRepository.class);
        LoginSessionRepository loginSessionRepository = Mockito.mock(LoginSessionRepository.class);

        SideShiftProperties properties = new SideShiftProperties();
        properties.setSecret("secret");
        properties.setAffiliateId("affiliate");

        ObjectMapper objectMapper = new ObjectMapper();
        SideShiftSwapService service = new SideShiftSwapService(
                client,
                properties,
                shiftRepository,
                objectMapper,
                loginSessionRepository
        );

        LoginSessionEntity session = new LoginSessionEntity();
        session.setSessionToken("token-123");
        session.setIpAddress("1.2.3.4");
        when(loginSessionRepository.findBySessionToken("token-123")).thenReturn(Optional.of(session));

        SideShiftShiftResponse response = new SideShiftShiftResponse(
                "shift-1",
                "deposit-addr",
                "btc",
                "bitcoin",
                "btc",
                "liquid"
        );
        when(client.createVariableShift(eq("secret"), eq("1.2.3.4"), Mockito.any()))
                .thenReturn(response);

        UUID processId = UUID.randomUUID();
        service.swapToLiquid("btc", "btc", "settle-addr", "refund-addr", processId, "token-123");

        ArgumentCaptor<SideShiftShiftEntity> captor = ArgumentCaptor.forClass(SideShiftShiftEntity.class);
        verify(shiftRepository).save(captor.capture());
        SideShiftShiftEntity saved = captor.getValue();
        assertThat(saved.getUserIp()).isEqualTo("1.2.3.4");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
