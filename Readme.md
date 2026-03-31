# Supply Chain Smart Contract - Hyperledger Fabric
 
**Project:** 3-Node Permissioned Supply Chain Network (Manufacturer -> Distributor -> Retailer)

---

## 1. Project Overview
This project implements a Hyperledger Fabric smart contract (Chaincode) written in Java to track the lifecycle of a product batch. It utilizes a 3-organization permissioned network:

* **Org1:** Manufacturer
* **Org2:** Distributor
* **Org3:** Retailer

The smart contract strictly enforces cryptographic access control using `ctx.getClientIdentity().getMSPID()`, ensuring that only the authorized organization can trigger specific state transitions (`CREATED` -> `IN_TRANSIT` -> `DELIVERED`).

---

## 2. Prerequisites & Environment
* **OS:** Ubuntu / Linux (WSL2 supported)
* **Engine:** Docker & Docker Compose V2 plugin
* **Language:** Java 11 (JDK & JRE)
* **Build Tool:** Gradle 8.5+ (Wrapper included in source)
* **Framework:** Hyperledger Fabric v2.5.x (`fabric-samples`)

---

## 3. Directory Structure
Before executing, ensure the Java chaincode is placed in the exact following structure within your `fabric-samples` directory:

```text
fabric-samples/chaincode/supplychain/java/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradle/
└── src/main/java/org/supplychain/
    ├── ProductBatch.java
    └── SupplyChainContract.java
    
## 4. Phase 1: Network Initialization & Deployment
Navigate to the test-network control directory and clean any stale Docker containers, then boot the foundation (Org1 & Org2) and deploy the compiled Java smart contract.

cd fabric-samples/test-network

# 1. Nuke stale environments and clear Docker volumes
./network.sh down
docker network prune -f
docker volume prune -f

# 2. Boot the Orderer, Org1, and Org2 with Certificate Authorities
./network.sh up createChannel -c supplychannel -ca

# 3. Compile, package, and deploy the Java Chaincode
./network.sh deployCC -ccn supplychain -ccp ../chaincode/supplychain/java -ccl java -c supplychannel

Note: Wait for the output: Chaincode definition committed on channel 'supplychannel' before proceeding.

## 5. Phase 2: Injecting the Retailer (Org3)
The assignment requires a 3-node network. We dynamically inject Org3 into the live channel.

# 1. Generate Org3 crypto material and add to channel config
cd addOrg3
./addOrg3.sh up -c supplychannel -ca

# 2. Return to test-network directory
cd ..

# 3. Manually sync Org3 by fetching the Genesis Block via Org1's clearance
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org1.example.com/users/Admin@org1.example.com/msp](https://org1.example.com/users/Admin@org1.example.com/msp)
export CORE_PEER_ADDRESS=localhost:7051

peer channel fetch 0 channel-artifacts/supplychannel.block -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c supplychannel --tls --cafile ${PWD}/organizations/ordererOrganizations/[example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem](https://example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem)

# 4. Assume Org3 identity and physically join the channel
export CORE_PEER_LOCALMSPID="Org3MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org3.example.com/peers/peer0.org3.example.com/tls/ca.crt](https://org3.example.com/peers/peer0.org3.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org3.example.com/users/Admin@org3.example.com/msp](https://org3.example.com/users/Admin@org3.example.com/msp)
export CORE_PEER_ADDRESS=localhost:11051

peer channel join -b channel-artifacts/supplychannel.block

## 6. Phase 3: Executing the Supply Chain State Machine
To test the access control and state transitions, execute the following commands using the specific identities.

# Step A: Manufacturer Creates the Asset
Identity: Org1MSP

export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org1.example.com/users/Admin@org1.example.com/msp](https://org1.example.com/users/Admin@org1.example.com/msp)
export CORE_PEER_ADDRESS=localhost:7051

peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/[example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem](https://example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem) -C supplychannel -n supplychain --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt) --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org2.example.com/peers/peer0.org2.example.com/tls/ca.crt](https://org2.example.com/peers/peer0.org2.example.com/tls/ca.crt) -c '{"function":"createBatch","Args":["BATCH_1001", "Secure_Processors"]}'

# Step B: Distributor Updates the Shipment
Identity: Org2MSP

export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org2.example.com/peers/peer0.org2.example.com/tls/ca.crt](https://org2.example.com/peers/peer0.org2.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org2.example.com/users/Admin@org2.example.com/msp](https://org2.example.com/users/Admin@org2.example.com/msp)
export CORE_PEER_ADDRESS=localhost:9051

peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/[example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem](https://example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem) -C supplychannel -n supplychain --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt) --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org2.example.com/peers/peer0.org2.example.com/tls/ca.crt](https://org2.example.com/peers/peer0.org2.example.com/tls/ca.crt) -c '{"function":"updateShipment","Args":["BATCH_1001"]}'

# Step C: Retailer Confirms Delivery
Identity: Org3MSP

export CORE_PEER_LOCALMSPID="Org3MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org3.example.com/peers/peer0.org3.example.com/tls/ca.crt](https://org3.example.com/peers/peer0.org3.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org3.example.com/users/Admin@org3.example.com/msp](https://org3.example.com/users/Admin@org3.example.com/msp)
export CORE_PEER_ADDRESS=localhost:11051

peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/[example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem](https://example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem) -C supplychannel -n supplychain --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt) --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/[org2.example.com/peers/peer0.org2.example.com/tls/ca.crt](https://org2.example.com/peers/peer0.org2.example.com/tls/ca.crt) -c '{"function":"confirmDelivery","Args":["BATCH_1001"]}'

## 7. Final Audit & Verification
To prove the complete execution of the assignment, pull the product history from the ledger. This utilizes the GetHistoryForKey API to return a timestamped array of all state modifications.

Identity: Org1MSP (Or Org2MSP)

export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/[org1.example.com/peers/peer0.org1.example.com/tls/ca.crt](https://org1.example.com/peers/peer0.org1.example.com/tls/ca.crt)
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/[org1.example.com/users/Admin@org1.example.com/msp](https://org1.example.com/users/Admin@org1.example.com/msp)
export CORE_PEER_ADDRESS=localhost:7051

peer chaincode query -C supplychannel -n supplychain -c '{"function":"getProductHistory","Args":["BATCH_1001"]}'

Expected Output: A 3-part JSON array showing sequential state changes (CREATED -> IN_TRANSIT -> DELIVERED) with unique TxIDs and timestamps.

## 8. Network Teardown
To safely halt the network and release resources:

./network.sh down
