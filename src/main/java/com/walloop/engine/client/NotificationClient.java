package com.walloop.engine.client;

import com.walloop.engine.dto.CustomerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "notification-service", url = "${clients.notification.url:http://localhost:8081}")
public interface NotificationClient {

    @PostMapping("/api/notifications/customers")
    void notifyCustomerCreated(CustomerDto dto);
}
