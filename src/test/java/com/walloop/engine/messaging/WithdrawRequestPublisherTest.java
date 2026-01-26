package com.walloop.engine.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class WithdrawRequestPublisherTest {

    @Test
    void publishesWithdrawRequestWithDestination() {
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        WithdrawRequestPublisher publisher = new WithdrawRequestPublisher(rabbitTemplate);
        UUID processId = UUID.randomUUID();

        publisher.publish(processId);

        ArgumentCaptor<WithdrawRequestMessage> messageCaptor = ArgumentCaptor.forClass(WithdrawRequestMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CoreMessagingConfiguration.CORE_EXCHANGE),
                eq(WithdrawMessagingConfiguration.BALANCE_PROCESS_ROUTING_KEY),
                messageCaptor.capture()
        );
        WithdrawRequestMessage message = messageCaptor.getValue();
        assertThat(message.processId()).isEqualTo(processId);
        assertThat(message.destination()).isEqualTo("TO_LIQUID_NETWORK");
    }
}
