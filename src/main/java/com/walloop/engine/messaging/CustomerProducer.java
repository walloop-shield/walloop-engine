package com.walloop.engine.messaging;

import com.walloop.engine.dto.CustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerProducer {

    public static final String EXCHANGE = "customer.exchange";
    public static final String ROUTING_KEY_CREATED = "customer.created";

    private final RabbitTemplate rabbitTemplate;

    public void publishCustomerCreated(CustomerDto customerDto) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_CREATED, customerDto);
    }
}
