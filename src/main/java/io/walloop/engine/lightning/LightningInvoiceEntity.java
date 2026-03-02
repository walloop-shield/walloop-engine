package io.walloop.engine.lightning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lightning_invoice", schema = "engine")
public class LightningInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID processId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String invoice;

    @Column
    private String balanceBtc;

    @Column
    private Long balanceSats;

    @Column
    private Long balanceMsats;

    @Column
    private String balanceUsdt;

    @Column
    private String swapPartner;

    @Column(name = "swap_id")
    private String swapId;

    @Column
    private String swapLockupAddress;

    @Column
    private Long swapExpectedAmount;

    @Column
    private Double swapFeePercentage;

    @Column
    private Long swapMinerFees;

    @Column
    private String swapPairHash;

    @Column
    private Long liquidFeeSats;

    @Column
    private Integer liquidFeeConfTarget;

    @Column
    private Integer liquidFeeVbytes;

    @Column
    private String liquidTxId;

    @Column(columnDefinition = "TEXT")
    private String swapRequestPayload;

    @Column(columnDefinition = "TEXT")
    private String swapResponsePayload;

    @Column
    private String swapStatus;

    @Column(columnDefinition = "TEXT")
    private String swapDecodedTransactionPayload;

    @Column
    private Long swapPaidAmountSats;

    @Column(columnDefinition = "TEXT")
    private String swapStatusPayload;

    private OffsetDateTime swapPaidAt;

    @Column
    private String swapClaimPublicKey;

    @Column(columnDefinition = "TEXT")
    private String swapClaimTree;

    @Column
    private String swapClaimPubNonce;

    @Column
    private String swapClaimTxHash;

    @Column
    private String swapClaimPartialSignature;

    @Column
    private String swapClaimStatus;

    private OffsetDateTime swapClaimSubmittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LightningInvoiceStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public String getBalanceBtc() {
        return balanceBtc;
    }

    public void setBalanceBtc(String balanceBtc) {
        this.balanceBtc = balanceBtc;
    }

    public Long getBalanceSats() {
        return balanceSats;
    }

    public void setBalanceSats(Long balanceSats) {
        this.balanceSats = balanceSats;
    }

    public Long getBalanceMsats() {
        return balanceMsats;
    }

    public void setBalanceMsats(Long balanceMsats) {
        this.balanceMsats = balanceMsats;
    }

    public String getBalanceUsdt() {
        return balanceUsdt;
    }

    public void setBalanceUsdt(String balanceUsdt) {
        this.balanceUsdt = balanceUsdt;
    }

    public String getSwapPartner() {
        return swapPartner;
    }

    public void setSwapPartner(String swapPartner) {
        this.swapPartner = swapPartner;
    }

    public String getSwapId() {
        return swapId;
    }

    public void setSwapId(String swapId) {
        this.swapId = swapId;
    }

    public String getSwapLockupAddress() {
        return swapLockupAddress;
    }

    public void setSwapLockupAddress(String swapLockupAddress) {
        this.swapLockupAddress = swapLockupAddress;
    }

    public Long getSwapExpectedAmount() {
        return swapExpectedAmount;
    }

    public void setSwapExpectedAmount(Long swapExpectedAmount) {
        this.swapExpectedAmount = swapExpectedAmount;
    }

    public Double getSwapFeePercentage() {
        return swapFeePercentage;
    }

    public void setSwapFeePercentage(Double swapFeePercentage) {
        this.swapFeePercentage = swapFeePercentage;
    }

    public Long getSwapMinerFees() {
        return swapMinerFees;
    }

    public void setSwapMinerFees(Long swapMinerFees) {
        this.swapMinerFees = swapMinerFees;
    }

    public String getSwapPairHash() {
        return swapPairHash;
    }

    public void setSwapPairHash(String swapPairHash) {
        this.swapPairHash = swapPairHash;
    }

    public Long getLiquidFeeSats() {
        return liquidFeeSats;
    }

    public void setLiquidFeeSats(Long liquidFeeSats) {
        this.liquidFeeSats = liquidFeeSats;
    }

    public Integer getLiquidFeeConfTarget() {
        return liquidFeeConfTarget;
    }

    public void setLiquidFeeConfTarget(Integer liquidFeeConfTarget) {
        this.liquidFeeConfTarget = liquidFeeConfTarget;
    }

    public Integer getLiquidFeeVbytes() {
        return liquidFeeVbytes;
    }

    public void setLiquidFeeVbytes(Integer liquidFeeVbytes) {
        this.liquidFeeVbytes = liquidFeeVbytes;
    }

    public String getLiquidTxId() {
        return liquidTxId;
    }

    public void setLiquidTxId(String liquidTxId) {
        this.liquidTxId = liquidTxId;
    }

    public String getSwapRequestPayload() {
        return swapRequestPayload;
    }

    public void setSwapRequestPayload(String swapRequestPayload) {
        this.swapRequestPayload = swapRequestPayload;
    }

    public String getSwapResponsePayload() {
        return swapResponsePayload;
    }

    public void setSwapResponsePayload(String swapResponsePayload) {
        this.swapResponsePayload = swapResponsePayload;
    }

    public String getSwapStatus() {
        return swapStatus;
    }

    public void setSwapStatus(String swapStatus) {
        this.swapStatus = swapStatus;
    }

    public String getSwapDecodedTransactionPayload() {
        return swapDecodedTransactionPayload;
    }

    public void setSwapDecodedTransactionPayload(String swapDecodedTransactionPayload) {
        this.swapDecodedTransactionPayload = swapDecodedTransactionPayload;
    }

    public Long getSwapPaidAmountSats() {
        return swapPaidAmountSats;
    }

    public void setSwapPaidAmountSats(Long swapPaidAmountSats) {
        this.swapPaidAmountSats = swapPaidAmountSats;
    }

    public String getSwapStatusPayload() {
        return swapStatusPayload;
    }

    public void setSwapStatusPayload(String swapStatusPayload) {
        this.swapStatusPayload = swapStatusPayload;
    }

    public OffsetDateTime getSwapPaidAt() {
        return swapPaidAt;
    }

    public void setSwapPaidAt(OffsetDateTime swapPaidAt) {
        this.swapPaidAt = swapPaidAt;
    }

    public String getSwapClaimPublicKey() {
        return swapClaimPublicKey;
    }

    public void setSwapClaimPublicKey(String swapClaimPublicKey) {
        this.swapClaimPublicKey = swapClaimPublicKey;
    }

    public String getSwapClaimTree() {
        return swapClaimTree;
    }

    public void setSwapClaimTree(String swapClaimTree) {
        this.swapClaimTree = swapClaimTree;
    }

    public String getSwapClaimPubNonce() {
        return swapClaimPubNonce;
    }

    public void setSwapClaimPubNonce(String swapClaimPubNonce) {
        this.swapClaimPubNonce = swapClaimPubNonce;
    }

    public String getSwapClaimTxHash() {
        return swapClaimTxHash;
    }

    public void setSwapClaimTxHash(String swapClaimTxHash) {
        this.swapClaimTxHash = swapClaimTxHash;
    }

    public String getSwapClaimPartialSignature() {
        return swapClaimPartialSignature;
    }

    public void setSwapClaimPartialSignature(String swapClaimPartialSignature) {
        this.swapClaimPartialSignature = swapClaimPartialSignature;
    }

    public String getSwapClaimStatus() {
        return swapClaimStatus;
    }

    public void setSwapClaimStatus(String swapClaimStatus) {
        this.swapClaimStatus = swapClaimStatus;
    }

    public OffsetDateTime getSwapClaimSubmittedAt() {
        return swapClaimSubmittedAt;
    }

    public void setSwapClaimSubmittedAt(OffsetDateTime swapClaimSubmittedAt) {
        this.swapClaimSubmittedAt = swapClaimSubmittedAt;
    }

    public LightningInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(LightningInvoiceStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

