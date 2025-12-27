package com.walloop.engine.messaging;

import java.util.UUID;

public record WithdrawRequestMessage(UUID processId, String destination) {
}
