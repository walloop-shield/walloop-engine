package com.walloop.engine.boltz;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoltzSubmarineRequest {
    String from;
    String to;
    String invoice;
    String preimageHash;
    String refundPublicKey;
    String pairHash;
    String referralId;
    Integer paymentTimeout;
}
