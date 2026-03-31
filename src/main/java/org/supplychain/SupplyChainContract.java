package org.supplychain;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;
import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "SupplyChainContract",
        info = @Info(
                title = "Supply Chain Smart Contract",
                description = "Tracks product batches from Manufacturer to Retailer",
                version = "1.0.0"
        )
)
@Default
public final class SupplyChainContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum SupplyChainErrors {
        BATCH_ALREADY_EXISTS,
        BATCH_NOT_FOUND,
        UNAUTHORIZED_ACCESS,
        INVALID_STATE_TRANSITION
    }

    /**
     * Org1 (Manufacturer) creates the batch.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public ProductBatch createBatch(final Context ctx, final String batchId, final String productName) {
        // ACCESS CONTROL: Only Manufacturer (Org1) can create
        String clientMSP = ctx.getClientIdentity().getMSPID();
        if (!clientMSP.equals("Org1MSP")) {
            throw new ChaincodeException("Only the Manufacturer can create a batch. Caller MSP: " + clientMSP, 
                                         SupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        if (batchExists(ctx, batchId)) {
            throw new ChaincodeException("Batch " + batchId + " already exists", 
                                         SupplyChainErrors.BATCH_ALREADY_EXISTS.toString());
        }

        ProductBatch batch = new ProductBatch(batchId, productName, clientMSP, "Manufacturer", "CREATED");
        String batchState = genson.serialize(batch);
        ctx.getStub().putStringState(batchId, batchState);

        return batch;
    }

    /**
     * Org2 (Distributor) updates shipment status.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public ProductBatch updateShipment(final Context ctx, final String batchId) {
        // ACCESS CONTROL: Only Distributor (Org2) can pick up
        String clientMSP = ctx.getClientIdentity().getMSPID();
        if (!clientMSP.equals("Org2MSP")) {
            throw new ChaincodeException("Only the Distributor can update shipment. Caller MSP: " + clientMSP, 
                                         SupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        ProductBatch batch = getBatch(ctx, batchId);

        if (!batch.getStatus().equals("CREATED")) {
            throw new ChaincodeException("Batch must be in CREATED state to be shipped.", 
                                         SupplyChainErrors.INVALID_STATE_TRANSITION.toString());
        }

        batch.setCurrentOwner("Distributor");
        batch.setStatus("IN_TRANSIT");

        String newBatchState = genson.serialize(batch);
        ctx.getStub().putStringState(batchId, newBatchState);

        return batch;
    }

    /**
     * Org3 (Retailer) confirms final delivery.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public ProductBatch confirmDelivery(final Context ctx, final String batchId) {
        // ACCESS CONTROL: Only Retailer (Org3) can confirm delivery
        String clientMSP = ctx.getClientIdentity().getMSPID();
        if (!clientMSP.equals("Org3MSP")) {
            throw new ChaincodeException("Only the Retailer can confirm delivery. Caller MSP: " + clientMSP, 
                                         SupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        ProductBatch batch = getBatch(ctx, batchId);

        if (!batch.getStatus().equals("IN_TRANSIT")) {
            throw new ChaincodeException("Batch must be IN_TRANSIT to be delivered.", 
                                         SupplyChainErrors.INVALID_STATE_TRANSITION.toString());
        }

        batch.setCurrentOwner("Retailer");
        batch.setStatus("DELIVERED");

        String newBatchState = genson.serialize(batch);
        ctx.getStub().putStringState(batchId, newBatchState);

        return batch;
    }

    /**
     * Retrieves the complete lifecycle history of the product.
     * MEETS ASSIGNMENT REQUIREMENT: Returns TxID, Timestamp, and State.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getProductHistory(final Context ctx, final String batchId) {
        List<String> history = new ArrayList<>();
        
        // Use Fabric's GetHistoryForKey API
        QueryResultsIterator<KeyModification> results = ctx.getStub().getHistoryForKey(batchId);
        
        for (KeyModification modification : results) {
            String txId = modification.getTxId();
            String timestamp = modification.getTimestamp().toString();
            String value = modification.getStringValue();
            
            // Format into a clean JSON representation
            String record = String.format("{\"TxId\":\"%s\", \"Timestamp\":\"%s\", \"Record\":%s}", 
                                          txId, timestamp, value);
            history.add(record);
        }
        
        return history.toString();
    }

    // --- Helper Functions ---
    private boolean batchExists(final Context ctx, final String batchId) {
        byte[] buffer = ctx.getStub().getState(batchId);
        return (buffer != null && buffer.length > 0);
    }

    private ProductBatch getBatch(final Context ctx, final String batchId) {
        String batchState = ctx.getStub().getStringState(batchId);
        if (batchState == null || batchState.isEmpty()) {
            throw new ChaincodeException("Batch " + batchId + " does not exist", 
                                         SupplyChainErrors.BATCH_NOT_FOUND.toString());
        }
        return genson.deserialize(batchState, ProductBatch.class);
    }
}