package com.walloop.engine.messaging;

import com.walloop.engine.dto.CustomerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerConsumer {

    @RabbitListener(queues = CustomerMessagingConfiguration.QUEUE_CREATED)
    public void handleCustomerCreated(CustomerDto dto) {
        log.info("Received customer created event: {}", dto);
    }
}
