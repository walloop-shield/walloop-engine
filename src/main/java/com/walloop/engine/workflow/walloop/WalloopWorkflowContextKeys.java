package com.walloop.engine.workflow.walloop;

public final class WalloopWorkflowContextKeys {

    private WalloopWorkflowContextKeys() {
    }

    public static final String TRANSACTION_ID = "transactionId";
    public static final String OWNER_ID = "ownerId";
    public static final String WALLOOP_DEPOSIT_DETECTED = "walloopDepositDetected";
    public static final String CHAIN = "chain";
    public static final String CORRELATED_ADDRESS = "correlatedAddress";
    public static final String NEW_ADDRESS = "newAddress";
    public static final String LIQUID_ADDRESS = "liquidAddress";
    public static final String DERIVATION_PATH = "derivationPath";
    public static final String FEE_SATS = "feeSats";
    public static final String SWAP_ID = "swapId";
    public static final String SWAP_DEPOSIT_ADDRESS = "swapDepositAddress";
}
