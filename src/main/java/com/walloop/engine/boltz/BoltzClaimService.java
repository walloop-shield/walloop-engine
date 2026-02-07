package com.walloop.engine.boltz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import java.time.OffsetDateTime;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.message.PayReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoltzClaimService {

    private final BoltzClient boltzClient;
    private final ObjectMapper objectMapper;
    private final SynchronousLndAPI lndApi;
    private final LiquidWalletRepository liquidWalletRepository;
    private final BoltzClaimSigner claimSigner;
    private final LightningInvoiceRepository lightningInvoiceRepository;

    @Value("${boltz.claim.enabled:true}")
    private boolean enabled;

    @Value("${boltz.claim.pending-status:transaction.claim.pending}")
    private String pendingStatus;

    private static final String CLAIM_STATUS_PENDING = "PENDING";
    private static final String CLAIM_STATUS_SUBMITTED = "SUBMITTED";

    public boolean isClaimPending(BoltzSwapStatusResponse response) {
        if (response == null || response.status() == null) {
            return false;
        }
        return response.status().equalsIgnoreCase(pendingStatus);
    }

    public void tryClaim(LightningInvoiceEntity invoice) {
        if (!enabled) {
            return;
        }
        if (invoice == null || invoice.getSwapId() == null || invoice.getSwapId().isBlank()) {
            return;
        }
        String swapId = invoice.getSwapId();
        Optional<SwapClaimParams> params = resolveClaimParams(invoice);
        if (params.isEmpty()) {
            log.warn("BoltzClaimService - claim params missing swapId={}", swapId);
            return;
        }
        BoltzSubmarineClaimResponse claimDetails = boltzClient.getSubmarineClaim(swapId);
        if (claimDetails == null || claimDetails.preimage() == null || claimDetails.preimage().isBlank()) {
            log.warn("BoltzClaimService - claim details missing swapId={}", swapId);
            return;
        }
        updateClaimSnapshot(invoice, params.get(), claimDetails, null, CLAIM_STATUS_PENDING, null);
        if (!verifyPreimage(invoice, claimDetails.preimage())) {
            log.warn("BoltzClaimService - preimage mismatch swapId={}", swapId);
            return;
        }

        LiquidWalletEntity wallet = liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(invoice.getProcessId())
                .orElse(null);
        if (wallet == null || wallet.getPrivateKey() == null || wallet.getPrivateKey().isBlank()) {
            log.warn("BoltzClaimService - refund private key missing swapId={}", swapId);
            return;
        }

        BoltzClaimSigningRequest signingRequest = new BoltzClaimSigningRequest(
                swapId,
                params.get().claimPublicKey(),
                params.get().swapTree(),
                claimDetails.pubNonce(),
                claimDetails.transactionHash(),
                wallet.getPrivateKey()
        );
        Optional<BoltzClaimSignature> signature = claimSigner.sign(signingRequest);
        if (signature.isEmpty()) {
            log.warn("BoltzClaimService - claim signature missing swapId={}", swapId);
            return;
        }
        BoltzSubmarineClaimRequest submit = new BoltzSubmarineClaimRequest(
                signature.get().pubNonce(),
                signature.get().partialSignature()
        );
        boltzClient.submitSubmarineClaim(swapId, submit);
        updateClaimSnapshot(invoice, params.get(), claimDetails, signature.get(), CLAIM_STATUS_SUBMITTED, OffsetDateTime.now());
        log.info("BoltzClaimService - claim submitted swapId={}", swapId);
    }

    private Optional<SwapClaimParams> resolveClaimParams(LightningInvoiceEntity invoice) {
        String payload = invoice.getSwapResponsePayload();
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> json = objectMapper.readValue(payload, new TypeReference<>() {});
            Object claimKey = json.get("claimPublicKey");
            Object swapTree = json.get("swapTree");
            if (claimKey == null || swapTree == null) {
                return Optional.empty();
            }
            String claimPublicKey = claimKey.toString();
            String swapTreeValue = swapTree.toString();
            if (claimPublicKey.isBlank() || swapTreeValue.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new SwapClaimParams(claimPublicKey, swapTreeValue));
        } catch (Exception e) {
            log.warn("BoltzClaimService - claim params parse failed swapId={}", invoice.getSwapId(), e);
            return Optional.empty();
        }
    }

    private boolean verifyPreimage(LightningInvoiceEntity invoice, String preimageHex) {
        String paymentRequest = invoice.getInvoice();
        if (paymentRequest == null || paymentRequest.isBlank()) {
            return false;
        }
        try {
            PayReq payReq = lndApi.decodePayReq(paymentRequest);
            if (payReq == null || payReq.getPaymentHash() == null || payReq.getPaymentHash().isBlank()) {
                return false;
            }
            byte[] preimage = hexToBytes(preimageHex);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(preimage);
            String hashHex = bytesToHex(hash);
            return hashHex.equalsIgnoreCase(payReq.getPaymentHash());
        } catch (Exception e) {
            log.warn("BoltzClaimService - preimage verify failed swapId={}", invoice.getSwapId(), e);
            return false;
        }
    }

    private void updateClaimSnapshot(
            LightningInvoiceEntity invoice,
            SwapClaimParams params,
            BoltzSubmarineClaimResponse claimDetails,
            BoltzClaimSignature signature,
            String status,
            OffsetDateTime submittedAt
    ) {
        if (invoice == null || params == null || claimDetails == null) {
            return;
        }
        if (invoice.getSwapClaimPublicKey() == null) {
            invoice.setSwapClaimPublicKey(params.claimPublicKey());
        }
        if (invoice.getSwapClaimTree() == null) {
            invoice.setSwapClaimTree(params.swapTree());
        }
        invoice.setSwapClaimPubNonce(claimDetails.pubNonce());
        invoice.setSwapClaimTxHash(claimDetails.transactionHash());
        if (signature != null) {
            invoice.setSwapClaimPartialSignature(signature.partialSignature());
        }
        invoice.setSwapClaimStatus(status);
        if (submittedAt != null) {
            invoice.setSwapClaimSubmittedAt(submittedAt);
        }
        invoice.setUpdatedAt(OffsetDateTime.now());
        lightningInvoiceRepository.save(invoice);
    }

    private byte[] hexToBytes(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        int len = normalized.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(normalized.charAt(i), 16) << 4)
                    + Character.digit(normalized.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private record SwapClaimParams(String claimPublicKey, String swapTree) {
    }
}
