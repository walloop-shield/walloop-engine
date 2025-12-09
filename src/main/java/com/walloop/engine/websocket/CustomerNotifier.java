package com.walloop.engine.websocket;

import com.walloop.engine.dto.CustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyCreated(CustomerDto customer) {
        messagingTemplate.convertAndSend("/topic/customers", customer);
    }
}
