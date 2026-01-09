package com.walloop.engine.lightning;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.ChannelBalanceResponse;
import org.lightningj.lnd.wrapper.message.GetInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LightningInboundLiquidityService {

    private static final List<LightningInboundLiquidityRequestStatus> PENDING_STATUSES =
            List.of(LightningInboundLiquidityRequestStatus.REQUESTED);

    private final SynchronousLndAPI lndApi;
    private final LightningInboundLiquidityRequestRepository requestRepository;
    private final LspLiquidityService lspLiquidityService;

    @Value("${walloop.lightning.inbound-target-sats:10000000}")
    private long inboundTargetSats;

    @Value("${walloop.lightning.inbound-check-enabled:true}")
    private boolean inboundCheckEnabled;

    @Value("${walloop.lightning.inbound-retry-delay:5m}")
    private Duration retryDelay;

    @Value("${walloop.lightning.lsp.provider:amboss-magma}")
    private String provider;

    @Value("${walloop.lightning.lsp.node-pubkey:}")
    private String nodePubKeyOverride;

    @Value("${walloop.lightning.lsp.base-url:}")
    private String lspBaseUrl;

    @Value("${walloop.lightning.lsp.api-key:}")
    private String lspApiKey;

    public InboundLiquidityCheck ensureInboundLiquidity(UUID processId) {
        if (!inboundCheckEnabled) {
            return InboundLiquidityCheck.readyCheck();
        }

        long inboundSats = resolveInboundSats();
        if (inboundSats >= inboundTargetSats) {
            return InboundLiquidityCheck.readyCheck();
        }

        Optional<LightningInboundLiquidityRequestEntity> pending = requestRepository
                .findFirstByStatusInOrderByCreatedAtDesc(PENDING_STATUSES);
        if (pending.isPresent()) {
            return InboundLiquidityCheck.retryCheck(
                    "Inbound liquidity pending",
                    retryDelay
            );
        }

        if (lspBaseUrl == null || lspBaseUrl.isBlank() || lspApiKey == null || lspApiKey.isBlank()) {
            return InboundLiquidityCheck.retryCheck(
                    "Inbound liquidity below target and LSP not configured",
                    retryDelay
            );
        }

        long requestedSats = Math.max(0, inboundTargetSats - inboundSats);
        LightningInboundLiquidityRequestEntity entity = new LightningInboundLiquidityRequestEntity();
        entity.setProcessId(processId);
        entity.setProvider(provider);
        entity.setStatus(LightningInboundLiquidityRequestStatus.REQUESTED);
        entity.setTargetInboundSats(inboundTargetSats);
        entity.setCurrentInboundSats(inboundSats);
        entity.setRequestedSats(requestedSats);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        LspLiquidityRequest request = new LspLiquidityRequest(
                processId,
                resolveNodePubKey(),
                inboundTargetSats,
                inboundSats,
                requestedSats
        );

        try {
            LspLiquidityResponse response = lspLiquidityService.requestInboundLiquidity(request);
            entity.setExternalId(response.externalId());
            entity.setResponsePayload(response.responsePayload());
        } catch (Exception e) {
            entity.setStatus(LightningInboundLiquidityRequestStatus.FAILED);
            entity.setErrorMessage(e.getMessage());
            log.warn("Failed to request inbound liquidity processId={}", processId, e);
        }

        entity.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(entity);
        return InboundLiquidityCheck.retryCheck("Inbound liquidity requested", retryDelay);
    }

    private long resolveInboundSats() {
        try {
            ChannelBalanceResponse response = lndApi.channelBalance();
            long remote = response.getRemoteBalance() != null ? response.getRemoteBalance().getSat() : 0;
            long pending = response.getPendingOpenRemoteBalance() != null
                    ? response.getPendingOpenRemoteBalance().getSat()
                    : 0;
            return remote + pending;
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to read inbound liquidity from LND", e);
        }
    }

    private String resolveNodePubKey() {
        if (nodePubKeyOverride != null && !nodePubKeyOverride.isBlank()) {
            return nodePubKeyOverride;
        }
        try {
            GetInfoResponse info = lndApi.getInfo();
            return info.getIdentityPubkey();
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to resolve LND node pubkey", e);
        }
    }

    public record InboundLiquidityCheck(boolean ready, String detail, Duration retryAfter) {
        public static InboundLiquidityCheck readyCheck() {
            return new InboundLiquidityCheck(true, null, null);
        }

        public static InboundLiquidityCheck retryCheck(String detail, Duration retryAfter) {
            return new InboundLiquidityCheck(false, detail, retryAfter);
        }
    }
}
