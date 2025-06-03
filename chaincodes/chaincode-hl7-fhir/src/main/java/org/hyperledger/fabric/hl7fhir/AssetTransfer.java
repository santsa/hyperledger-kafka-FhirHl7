package org.hyperledger.fabric.hl7fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(name = "hl7-fhir-java", info = @Info(title = "Asset Transfer", description = "The hyperlegendary asset transfer", version = "0.0.1-SNAPSHOT", license = @License(name = "Apache 2.0 License", url = "http://www.apache.org/licenses/LICENSE-2.0.html"), contact = @Contact(email = "a.transfer@example.com", name = "Adrian Transfer", url = "https://hyperledger.example.com")))
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    /*
     * static {
     * System.setProperty("https.protocols", "TLSv1.2");
     * System.setProperty("jdk.tls.client.enableStatusRequestExtension", "false");
     * }
     */

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS
    }

    // CHECKSTYLE:OFF
    public static final String VALUE_1 = "{\r\n  \"id\": \"asset1\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_2 = "{\r\n  \"id\": \"asset2\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    // CHECKSTYLE:ON

    /**
     * Creates some initial assets on the ledger.
     *
     * @param ctx the transaction context
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        CreateAsset(ctx, VALUE_1, "");
        CreateAsset(ctx, VALUE_2, "");
    }

    /**
     * Creates a new asset on the ledger.
     *
     * @param ctx            the transaction context
     * @param assetID        the ID of the new asset
     * @param color          the color of the new asset
     * @param size           the size for the new asset
     * @param owner          the owner of the new asset
     * @param appraisedValue the appraisedValue of the new asset
     * @return the created asset
     */
    @SuppressWarnings("unchecked")
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx, final String value, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, Object> valueMap = genson.deserialize(value, Map.class);
        if (AssetExists(ctx, valueMap.get("id").toString(), privateCollection)) {
            String errorMessage = String.format("Asset %s already exists", valueMap.get("id").toString());
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }
        Asset asset = new Asset(valueMap.get("id").toString(), valueMap);

        StringBuilder msg = new StringBuilder().append("CreateAsset - ");
        if (privateCollection != null && !privateCollection.isEmpty()) {
            stub.putPrivateData(privateCollection, asset.getId(), genson.serialize(asset));
            msg.append(privateCollection).append(" - ");
        } else {
            stub.putStringState(asset.getId(), genson.serialize(asset));
        }
        stub.setEvent(msg.append(ctx.getClientIdentity().getMSPID()).toString(), genson.serialize(asset).getBytes());
        return asset;
    }

    /**
     * Updates the properties of an asset on the ledger.
     *
     * @param ctx            the transaction context
     * @param assetID        the ID of the asset being updated
     * @param color          the color of the asset being updated
     * @param size           the size of the asset being updated
     * @param owner          the owner of the asset being updated
     * @param appraisedValue the appraisedValue of the asset being updated
     * @return the transferred asset
     */
    @SuppressWarnings("unchecked")
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset UpdateAsset(final Context ctx, final String value, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, Object> valueMap = genson.deserialize(value, Map.class);
        if (!AssetExists(ctx, valueMap.get("id").toString(), privateCollection)) {
            String errorMessage = String.format("Asset %s does not exist", valueMap.get("id").toString());
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        Asset currentAsset = getAsset(ctx, valueMap.get("id").toString(), privateCollection);
        Asset asset = new Asset(valueMap.get("id").toString(), valueMap);

        int versionId = Integer
                .parseInt(((Map<String, Object>) currentAsset.getValue().get("meta")).get("versionId").toString()) + 1;
        ((Map<String, Object>) asset.getValue().get("meta")).put("versionId", versionId + "");
        String assetJSON = genson.serialize(asset);

        StringBuilder msg = new StringBuilder().append("UpdateAsset - ");
        if (privateCollection != null && !privateCollection.isEmpty()) { // Corrected variable name
            stub.putPrivateData(privateCollection, asset.getId(), assetJSON);
            msg.append(privateCollection).append(" - ");
        } else {
            stub.putStringState(asset.getId(), assetJSON);
        }
        stub.setEvent(msg.append(ctx.getClientIdentity().getMSPID()).toString(), genson.serialize(asset).getBytes());
        return asset;
    }

    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx     the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String assetID, final String privateCollection) {
        return getAsset(ctx, assetID, privateCollection);
    }

    private Asset getAsset(final Context ctx, final String assetID, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = null;
        if (privateCollection != null && !privateCollection.isEmpty()) {
            byte[] assetBytes = stub.getPrivateData(privateCollection, assetID);
            if (assetBytes != null) {
                assetJSON = new String(assetBytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } else {
            assetJSON = stub.getStringState(assetID);
        }

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        return genson.deserialize(assetJSON, Asset.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAssetHistory(final Context ctx, final String assetID, final String privateCollection) {
        QueryResultsIterator<KeyModification> historyIterator = ctx.getStub().getHistoryForKey(assetID);
        List<Asset> assetHistoryList = new ArrayList<>();
        for (KeyModification modification : historyIterator) {
            String assetJSON = modification.getStringValue();
            System.out.println("Transaction ID: " + modification.getTxId());
            System.out.println("Timestamp: " + modification.getTimestamp().toString());
            System.out.println("Deleted: " + modification.isDeleted());
            assetHistoryList.add(genson.deserialize(assetJSON, Asset.class));
        }
        if (assetHistoryList.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        return genson.serialize(assetHistoryList);
    }

    /**
     * Deletes asset on the ledger.
     *
     * @param ctx     the transaction context
     * @param assetID the ID of the asset being deleted
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset DeleteAsset(final Context ctx, final String assetID, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        if (!AssetExists(ctx, assetID, privateCollection)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        Asset currentAsset = getAsset(ctx, assetID, privateCollection);
        currentAsset.getValue().put("active", false);
        StringBuilder msg = new StringBuilder().append("DeleteAsset - ");
        String assetJSON = genson.serialize(currentAsset);
        if (privateCollection != null && !privateCollection.isEmpty()) {
            stub.putPrivateData(privateCollection, assetID, assetJSON);
            stub.delPrivateData(privateCollection, assetID);
            msg.append(privateCollection).append(" - ");
        } else {
            stub.putStringState(assetID, assetJSON);
            stub.delState(assetID);
        }
        stub.setEvent(msg.append(ctx.getClientIdentity().getMSPID()).toString(),
                genson.serialize(currentAsset).getBytes());
        return currentAsset;
    }

    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx     the transaction context
     * @param assetID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String assetID, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        if (privateCollection != null && !privateCollection.isEmpty()) {
            byte[] assetBytes = stub.getPrivateData(privateCollection, assetID);
            return (assetBytes != null && assetBytes.length > 0);
        }
        String assetJSON = stub.getStringState(assetID);
        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx, final String privateCollection) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, Asset> resultAssetsMap = GetAllAssetsUnified(stub, null);
        Map<String, Asset> privateAssetsMap = GetAllAssetsUnified(stub, privateCollection);
        privateAssetsMap.forEach((k, v) -> resultAssetsMap.put(k.toString(), v));
        return genson.serialize(resultAssetsMap.values());
    }

    public String GetAllAssetsPublic(final Context ctx) {
        return genson.serialize(GetAllAssetsUnified(ctx.getStub(), null).values());
    }

    public String GetAllAssetsPrivate(final Context ctx, final String privateCollection) {
        return genson.serialize(GetAllAssetsUnified(ctx.getStub(), privateCollection).values());
    }

    private Map GetAllAssetsUnified(final ChaincodeStub stub, final String privateCollection) {
        Map<String, Asset> finalAssetsMap = new HashMap<>();
        QueryResultsIterator<KeyValue> results = (privateCollection != null && !privateCollection.isEmpty())
                ? stub.getPrivateDataByRange(privateCollection, "", "")
                : stub.getStateByRange("", "");
        for (KeyValue result : results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            System.out.println(asset);
            finalAssetsMap.put(asset.getId(), asset);
        }
        return finalAssetsMap;
    }

}
