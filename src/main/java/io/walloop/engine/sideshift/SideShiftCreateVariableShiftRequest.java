package io.walloop.engine.sideshift;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SideShiftCreateVariableShiftRequest {
    String depositCoin;
    String depositNetwork;
    String settleCoin;
    String settleNetwork;
    String settleAddress;
    String affiliateId;
    String refundAddress;
    String refundMemo;
    String settleMemo;
    Double commissionRate;
    String externalId;
}

