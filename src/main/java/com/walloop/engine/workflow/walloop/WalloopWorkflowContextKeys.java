package com.walloop.engine.workflow.walloop;

public final class WalloopWorkflowContextKeys {

    private WalloopWorkflowContextKeys() {
    }

    public static final String PROCESS_ID = "processId";
    public static final String OWNER_ID = "ownerId";
    public static final String CHAIN = "chain";
    public static final String CORRELATED_ADDRESS = "correlatedAddress";
    public static final String TRANSACTION_ADDRESS = "transactionAddress";
    public static final String LIQUID_ADDRESS = "liquidAddress";
    public static final String DERIVATION_PATH = "derivationPath";
    public static final String FEE = "fee";
    public static final String SWAP_ID = "swapId";
    public static final String SWAP_DEPOSIT_ADDRESS = "swapDepositAddress";
    public static final String SESSION_TOKEN = "sessionToken";
    public static final String LIGHTNING_INVOICE = "lightningInvoice";
}
