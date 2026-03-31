package org.supplychain;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class ProductBatch {

    @Property()
    private final String batchId;

    @Property()
    private final String productName;

    @Property()
    private final String manufacturer;

    @Property()
    private String currentOwner;

    @Property()
    private String status;

    public ProductBatch(@JsonProperty("batchId") final String batchId, 
                        @JsonProperty("productName") final String productName, 
                        @JsonProperty("manufacturer") final String manufacturer,
                        @JsonProperty("currentOwner") final String currentOwner,
                        @JsonProperty("status") final String status) {
        this.batchId = batchId;
        this.productName = productName;
        this.manufacturer = manufacturer;
        this.currentOwner = currentOwner;
        this.status = status;
    }

    public String getBatchId() { return batchId; }
    public String getProductName() { return productName; }
    public String getManufacturer() { return manufacturer; }
    public String getCurrentOwner() { return currentOwner; }
    public String getStatus() { return status; }

    public void setCurrentOwner(String currentOwner) { this.currentOwner = currentOwner; }
    public void setStatus(String status) { this.status = status; }
}