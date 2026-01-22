package com.walloop.engine.api;

import com.walloop.engine.lightning.LspWebhookService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/amboss")
@Slf4j
public class LspWebhookController {

    private final LspWebhookService webhookService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> handle(@RequestBody Map<String, Object> payload) {
        webhookService.handleAmbossWebhook(payload);
        return Map.of("status", "accepted");
    }
}
