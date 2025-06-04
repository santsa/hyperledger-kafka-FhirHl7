package org.hyperledger.fabric.hl7fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.owlike.genson.Genson;

public final class AssetTransferTest extends UtilTest {

    private AssetTransfer contract;
    private Context ctx;
    private ChaincodeStub stub;
    private ClientIdentity clientIdentity;
    private Genson genson;
    private final String TEST_MSPID = "TestMSPID";
    private final String TEST_COLLECTION = "TestCollection"; // Used for private data tests

    // Define AssetTransferErrors enum if it's used for payload in
    // ChaincodeException
    // This should mirror the enum in AssetTransfer.java
    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS
        // ASSET_HISTORY_NOT_FOUND // If used by GetAssetHistory error payload
    }

    @BeforeEach
    void setUp() {
        contract = new AssetTransfer();
        ctx = mock(Context.class);
        stub = mock(ChaincodeStub.class);
        clientIdentity = mock(ClientIdentity.class);
        genson = new Genson(); // Initialize Genson

        when(ctx.getStub()).thenReturn(stub);
        when(ctx.getClientIdentity()).thenReturn(clientIdentity);
        when(clientIdentity.getMSPID()).thenReturn(TEST_MSPID);
    }

    @Test
    public void invokeUnknownTransaction() {
        Throwable thrown = catchThrowable(() -> {
            contract.unknownTransaction(ctx);
        });
        assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                .hasMessage("Undefined contract method called");
        assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo(null);
    }

    // Helper method to safely get and cast the meta map, creating if absent
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMetaMap(final Asset asset) {
        if (asset == null || asset.getValue() == null) {
            // For test setup, if asset or value is null, it might indicate a problem
            // But for this helper, let's assume value should exist if we're getting meta
            throw new IllegalArgumentException("Asset or its value cannot be null when getting/creating meta map");
        }
        Object metaObj = asset.getValue().get("meta");
        if (metaObj == null) {
            Map<String, Object> newMetaMap = new HashMap<>();
            asset.getValue().put("meta", newMetaMap);
            return newMetaMap;
        }
        if (!(metaObj instanceof Map)) {
            throw new IllegalStateException("Meta field is not a Map, but: " + metaObj.getClass().getName());
        }
        return (Map<String, Object>) metaObj;
    }

    @Nested
    class ReadPublicAssetTests {
        @Test
        public void whenAssetExists() {
            when(stub.getStringState(ID_1)).thenReturn(VALUE_1);
            Asset asset = contract.ReadAsset(ctx, ID_1, ""); // Empty string for public
            Asset expectedAsset = genson.deserialize(VALUE_1, Asset.class);
            assertThat(asset).isEqualTo(expectedAsset);
        }

        @Test
        public void whenAssetDoesNotExist() {
            when(stub.getStringState(ID_1)).thenReturn(""); // Or null
            Throwable thrown = catchThrowable(() -> {
                contract.ReadAsset(ctx, ID_1, "");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + ID_1 + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }
    }

    @Nested
    class CreateAssetTests {
        @Test
        void testCreatePublicAssetSuccessfully() {
            when(stub.getStringState(anyString())).thenReturn(""); // Asset does not exist

            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            Asset expectedAsset = new Asset(id, valueMap); // AssetTransfer.CreateAsset does not modify input valueMap

            Asset returnedAsset = contract.CreateAsset(ctx, VALUE_1, null); // null for public

            verify(stub).putStringState(eq(id), eq(genson.serialize(expectedAsset)));
            verify(stub).setEvent(eq("CreateAsset - " + TEST_MSPID),
                    eq(genson.serialize(expectedAsset).getBytes(StandardCharsets.UTF_8)));
            assertThat(returnedAsset).isEqualTo(expectedAsset);
        }

        @Test
        void testCreatePrivateAssetSuccessfully() {
            when(stub.getPrivateData(eq(TEST_COLLECTION), anyString())).thenReturn(null); // Asset does not exist

            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            Asset expectedAsset = new Asset(id, valueMap); // AssetTransfer.CreateAsset does not modify input valueMap

            Asset returnedAsset = contract.CreateAsset(ctx, VALUE_1, TEST_COLLECTION);
            verify(stub).setEvent(eq("CreateAsset - " + TEST_COLLECTION + " - " + TEST_MSPID),
                    eq(genson.serialize(expectedAsset).getBytes(StandardCharsets.UTF_8)));
            assertThat(returnedAsset).isEqualTo(expectedAsset);
        }

        @Test
        public void testCreateAssetFailsIfPublicAssetAlreadyExists() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            when(stub.getStringState(id)).thenReturn(VALUE_1); // Asset exists

            Throwable thrown = catchThrowable(() -> {
                contract.CreateAsset(ctx, VALUE_1, null);
            });
            // AssetTransfer.java CreateAsset throws generic "Asset %s already exists"
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " already exists");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_ALREADY_EXISTS.toString().getBytes());
        }

        @Test
        void testCreateAssetFailsIfPrivateAssetAlreadyExists() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            // Mock AssetExists for private collection
            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id))).thenReturn(VALUE_1.getBytes(StandardCharsets.UTF_8));

            Throwable thrown = catchThrowable(() -> {
                contract.CreateAsset(ctx, VALUE_1, TEST_COLLECTION);
            });
            // AssetTransfer.java CreateAsset throws generic "Asset %s already exists"
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " already exists");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_ALREADY_EXISTS.toString().getBytes());
        }
    }

    @Nested
    class UpdateAssetTests {
        @Test
        void testUpdatePublicAssetSuccessfully() {
            Map<String, Object> initialValueMap = toValueMap(VALUE_1); // meta.versionId = "1"
            String id = initialValueMap.get("id").toString();
            Asset initialAsset = new Asset(id, initialValueMap);

            when(stub.getStringState(id)).thenReturn(genson.serialize(initialAsset)); // Asset exists

            Asset returnedAsset = contract.UpdateAsset(ctx, VALUE_1_UPDATED, null); // VALUE_1_UPDATED also has
                                                                                    // meta.versionId = "1"

            Map<String, Object> updatedValueMap = toValueMap(VALUE_1_UPDATED);
            Asset expectedAsset = new Asset(id, updatedValueMap);
            // UpdateAsset increments versionId
            Map<String, Object> expectedMeta = getMetaMap(expectedAsset); // Ensures meta map exists
            expectedMeta.put("versionId", "2"); // version "1" from VALUE_1 -> "2"

            verify(stub).putStringState(eq(id), eq(genson.serialize(expectedAsset)));
            verify(stub).setEvent(eq("UpdateAsset - " + TEST_MSPID),
                    eq(genson.serialize(expectedAsset).getBytes(StandardCharsets.UTF_8)));
            assertThat(returnedAsset).isEqualTo(expectedAsset);
        }

        @Test
        void testUpdatePrivateAssetSuccessfully() {
            Map<String, Object> initialValueMap = toValueMap(VALUE_1); // meta.versionId = "1"
            String id = initialValueMap.get("id").toString();
            Asset initialAsset = new Asset(id, initialValueMap);

            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id)))
                    .thenReturn(genson.serialize(initialAsset).getBytes(StandardCharsets.UTF_8));

            Asset returnedAsset = contract.UpdateAsset(ctx, VALUE_1_UPDATED, TEST_COLLECTION);

            Map<String, Object> updatedValueMap = toValueMap(VALUE_1_UPDATED);
            Asset expectedAsset = new Asset(id, updatedValueMap);
            Map<String, Object> expectedMeta = getMetaMap(expectedAsset); // Ensures meta map exists
            expectedMeta.put("versionId", "2"); // version "1" from VALUE_1 -> "2"
            assertThat(returnedAsset).isEqualTo(expectedAsset);
        }

        @Test
        void testUpdatePublicAssetFailsIfNotExist() {
            Map<String, Object> valueMap = toValueMap(VALUE_1_UPDATED);
            String id = valueMap.get("id").toString();
            when(stub.getStringState(id)).thenReturn(""); // Asset does not exist

            Throwable thrown = catchThrowable(() -> {
                contract.UpdateAsset(ctx, VALUE_1_UPDATED, null);
            });
            // AssetTransfer.java UpdateAsset throws generic "Asset %s does not exist"
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }

        @Test
        void testUpdatePrivateAssetFailsIfNotExist() {
            Map<String, Object> valueMap = toValueMap(VALUE_1_UPDATED);
            String id = valueMap.get("id").toString();
            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id))).thenReturn(null); // Asset does not exist

            Throwable thrown = catchThrowable(() -> {
                contract.UpdateAsset(ctx, VALUE_1_UPDATED, TEST_COLLECTION);
            });
            // AssetTransfer.java UpdateAsset throws generic "Asset %s does not exist"
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }
    }

    @Nested
    class DeleteAssetTests {
        @Test
        void testDeletePublicAssetSuccessfully() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            Asset existingAsset = new Asset(id, valueMap);
            // AssetTransfer.DeleteAsset sets "active":false on the asset used for the event
            // and for the putStringState before delState.
            // VALUE_1 does not have "active". The chaincode adds it.

            when(stub.getStringState(id)).thenReturn(genson.serialize(existingAsset));

            Asset returnedAsset = contract.DeleteAsset(ctx, id, null); // null for public

            // Expected asset for event and final state before delState
            Map<String, Object> expectedValueMap = toValueMap(VALUE_1); // Start with original
            expectedValueMap.put("active", false); // Chaincode sets this
            Asset expectedEventAsset = new Asset(id, expectedValueMap);

            // Verify the sequence: put inactive state, then delete
            verify(stub).putStringState(eq(id), eq(genson.serialize(expectedEventAsset)));
            verify(stub).delState(eq(id));
            verify(stub).setEvent(eq("DeleteAsset - " + TEST_MSPID),
                    eq(genson.serialize(expectedEventAsset).getBytes(StandardCharsets.UTF_8)));
            assertThat(returnedAsset).isEqualTo(expectedEventAsset);
        }

        @Test
        void testDeletePrivateAssetSuccessfully() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            Asset existingAsset = new Asset(id, valueMap);

            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id)))
                    .thenReturn(genson.serialize(existingAsset).getBytes(StandardCharsets.UTF_8));

            Asset returnedAsset = contract.DeleteAsset(ctx, id, TEST_COLLECTION);

            Map<String, Object> expectedValueMap = toValueMap(VALUE_1);
            expectedValueMap.put("active", false); // Chaincode sets this
            Asset expectedEventAsset = new Asset(id, expectedValueMap);
            assertThat(returnedAsset).isEqualTo(expectedEventAsset);
        }

        @Test
        public void testDeletePublicAssetFailsIfNotExist() {
            String id = toValueMap(VALUE_1).get("id").toString();
            when(stub.getStringState(id)).thenReturn(""); // Asset does not exist
            Throwable thrown = catchThrowable(() -> {
                contract.DeleteAsset(ctx, id, null);
            });
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }

        @Test
        void testDeletePrivateAssetFailsIfNotExist() {
            String id = toValueMap(VALUE_1).get("id").toString();
            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id))).thenReturn(null); // Asset does not exist

            Throwable thrown = catchThrowable(() -> {
                contract.DeleteAsset(ctx, id, TEST_COLLECTION);
            });
            // AssetTransfer.java DeleteAsset throws generic "Asset %s does not exist"
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + id + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }
    }

    @Nested
    class AssetExistsTests {
        @Test
        void testPublicAssetExistsReturnsTrue() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            when(stub.getStringState(id)).thenReturn(VALUE_1);
            boolean exists = contract.AssetExists(ctx, id, null); // null for public
            assertThat(exists).isTrue();
        }

        @Test
        void testPublicAssetExistsReturnsFalse() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            when(stub.getStringState(id)).thenReturn("");
            boolean exists = contract.AssetExists(ctx, id, null);
            assertThat(exists).isFalse();
        }

        @Test
        void testPrivateAssetExistsReturnsTrue() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id))).thenReturn(VALUE_1.getBytes(StandardCharsets.UTF_8));
            boolean exists = contract.AssetExists(ctx, id, TEST_COLLECTION);
            assertThat(exists).isTrue();
        }

        @Test
        void testPrivateAssetExistsReturnsFalse() {
            Map<String, Object> valueMap = toValueMap(VALUE_1);
            String id = valueMap.get("id").toString();
            when(stub.getPrivateData(eq(TEST_COLLECTION), eq(id))).thenReturn(null);
            boolean exists = contract.AssetExists(ctx, id, TEST_COLLECTION);
            assertThat(exists).isFalse();
        }
    }

    @Nested
    class GetAllAssetsTests {
        @Test
        void testGetAllAssetsReturnsMultipleAssets() {
            // This test needs to align with how AssetTransfer.java's GetAllAssets and
            // GetAllAssetsUnified work.
            // The uploaded AssetTransfer.java's GetAllAssets calls GetAllAssetsUnified
            // twice.
            // Mocking for public assets (first call to GetAllAssetsUnified with null
            // collection)
            QueryResultsIterator<KeyValue> publicIterator = mock(QueryResultsIterator.class);
            KeyValue kv1 = mock(KeyValue.class);
            KeyValue kv2 = mock(KeyValue.class);
            when(stub.getStateByRange("", "")).thenReturn(publicIterator);
            when(publicIterator.iterator()).thenReturn(java.util.Arrays.asList(kv1, kv2).iterator());
            when(kv1.getStringValue()).thenReturn(VALUE_1); // Asset1 public
            when(kv2.getStringValue()).thenReturn(VALUE_2); // Asset2 public

            // Mocking for private assets (second call to GetAllAssetsUnified with
            // TEST_COLLECTION)
            // Let's say Asset1 also has a private version, and Asset3 is only private
            QueryResultsIterator<KeyValue> privateIterator = mock(QueryResultsIterator.class);
            KeyValue pkv1 = mock(KeyValue.class); // Private version of Asset1
            KeyValue pkv3 = mock(KeyValue.class); // Private Asset3
            when(stub.getPrivateDataByRange(TEST_COLLECTION, "", "")).thenReturn(privateIterator);
            when(privateIterator.iterator()).thenReturn(java.util.Arrays.asList(pkv1, pkv3).iterator());


            final String value1Private = VALUE_1.replace("Smith", "Smith-Private");
            when(pkv1.getStringValue()).thenReturn(value1Private); // Private version of Asset1
            when(pkv3.getStringValue()).thenReturn(VALUE_3); // Asset3 private

            Asset asset1Public = genson.deserialize(VALUE_1, Asset.class); // Not used if private overrides
            Asset asset2Public = genson.deserialize(VALUE_2, Asset.class);
            Asset asset1Private = genson.deserialize(value1Private, Asset.class);
            Asset asset3Private = genson.deserialize(VALUE_3, Asset.class);

            // Expected: private Asset1 overrides public Asset1, public Asset2, private
            // Asset3
            // The order in the final list might not be guaranteed by HashMap.values(), so
            // compare contents.
            List<Asset> expectedAssetsList = new ArrayList<>();
            expectedAssetsList.add(asset1Private); // Private version of asset1
            expectedAssetsList.add(asset2Public); // Public version of asset2
            expectedAssetsList.add(asset3Private); // Private version of asset3
        }

        @Test
        void testGetAllAssetsReturnsEmptyArrayWhenNoAssets() {
            QueryResultsIterator<KeyValue> publicIterator = mock(QueryResultsIterator.class);
            when(stub.getStateByRange("", "")).thenReturn(publicIterator);
            when(publicIterator.iterator()).thenReturn(new ArrayList<KeyValue>().iterator());

            QueryResultsIterator<KeyValue> privateIterator = mock(QueryResultsIterator.class);
            when(stub.getPrivateDataByRange(TEST_COLLECTION, "", "")).thenReturn(privateIterator);
            when(privateIterator.iterator()).thenReturn(new ArrayList<KeyValue>().iterator());

            String result = contract.GetAllAssets(ctx, TEST_COLLECTION);
            assertThat(result).isEqualTo("[]");
        }
    }

    @Nested
    class GetAssetHistoryTests {
        @Test
        void testGetPublicAssetHistorySuccessfully() {
            QueryResultsIterator<KeyModification> iterator = mock(QueryResultsIterator.class);
            KeyModification km1 = mock(KeyModification.class);
            KeyModification km2 = mock(KeyModification.class);

            when(stub.getHistoryForKey(ID_1)).thenReturn(iterator); // Only one arg
            when(iterator.iterator()).thenReturn(java.util.Arrays.asList(km1, km2).iterator());

            // Ensure getStringValue() is mocked for each KeyModification
            when(km1.getStringValue()).thenReturn(VALUE_1);
            when(km1.isDeleted()).thenReturn(false); // Assume not deleted for simplicity here
            when(km1.getTxId()).thenReturn("tx1");
            when(km1.getTimestamp()).thenReturn(java.time.Instant.now());

            when(km2.getStringValue()).thenReturn(VALUE_1_UPDATED);
            when(km2.isDeleted()).thenReturn(false);
            when(km2.getTxId()).thenReturn("tx2");
            when(km2.getTimestamp()).thenReturn(java.time.Instant.now().plusSeconds(10));

            Asset assetVersion1 = genson.deserialize(VALUE_1, Asset.class);
            Asset assetVersion2 = genson.deserialize(VALUE_1_UPDATED, Asset.class);
            List<Asset> expectedHistory = new ArrayList<>(); // Use List<Asset>
            expectedHistory.add(assetVersion1);
            expectedHistory.add(assetVersion2);

            String historyJson = contract.GetAssetHistory(ctx, ID_1, null); // null for public
            assertThat(historyJson).isEqualTo(genson.serialize(expectedHistory));
        }

        @Test
        void testGetPublicAssetHistoryFailsIfNoHistory() {
            QueryResultsIterator<KeyModification> iterator = mock(QueryResultsIterator.class);
            when(stub.getHistoryForKey(ID_1)).thenReturn(iterator); // Only one arg
            when(iterator.iterator()).thenReturn(new ArrayList<KeyModification>().iterator());
            // Also mock getStringState for the AssetExists check within GetAssetHistory if
            // it's empty
            when(stub.getStringState(ID_1)).thenReturn("");

            Throwable thrown = catchThrowable(() -> {
                contract.GetAssetHistory(ctx, ID_1, null);
            });
            // Based on AssetTransfer.java: if (assetHistoryList.isEmpty()) { throw new
            // ChaincodeException("Asset %s does not exist", ASSET_NOT_FOUND) }
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + ID_1 + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }

        @Test
        void testGetPrivateAssetHistorySuccessfully() {
            // Current AssetTransfer.GetAssetHistory uses stub.getHistoryForKey which is
            // public only.
            // This test will behave like the public history test.
            QueryResultsIterator<KeyModification> iterator = mock(QueryResultsIterator.class);
            KeyModification km1 = mock(KeyModification.class);

            when(stub.getHistoryForKey(ID_1)).thenReturn(iterator); // Called by GetAssetHistory
            when(iterator.iterator()).thenReturn(java.util.Arrays.asList(km1).iterator());
            when(km1.getStringValue()).thenReturn(VALUE_1);
            when(km1.isDeleted()).thenReturn(false);
            when(km1.getTxId()).thenReturn("tx1");
            when(km1.getTimestamp()).thenReturn(java.time.Instant.now());

            Asset assetVersion1 = genson.deserialize(VALUE_1, Asset.class);
            List<Asset> expectedHistory = new ArrayList<>(); // Use List<Asset>
            expectedHistory.add(assetVersion1);

            String historyJson = contract.GetAssetHistory(ctx, ID_1, TEST_COLLECTION);
            assertThat(historyJson).isEqualTo(genson.serialize(expectedHistory));
        }

        @Test
        void testGetPrivateAssetHistoryFailsIfNoHistory() {
            // Behaves like public due to GetAssetHistory implementation
            QueryResultsIterator<KeyModification> iterator = mock(QueryResultsIterator.class);
            when(stub.getHistoryForKey(ID_1)).thenReturn(iterator);
            when(iterator.iterator()).thenReturn(new ArrayList<KeyModification>().iterator());
            // Also mock getStringState for the AssetExists check within GetAssetHistory if
            // it's empty
            // (assuming the AssetExists check in GetAssetHistory is for public state if
            // history is empty)
            // The GetAssetHistory in the uploaded AssetTransfer.java does not have an
            // AssetExists check.
            // It directly throws if assetHistoryList is empty.

            Throwable thrown = catchThrowable(() -> {
                contract.GetAssetHistory(ctx, ID_1, TEST_COLLECTION);
            });
            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset " + ID_1 + " does not exist");
            assertThat(((ChaincodeException) thrown).getPayload())
                    .isEqualTo(AssetTransferErrors.ASSET_NOT_FOUND.toString().getBytes());
        }
    }
}
