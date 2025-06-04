package org.hyperledger.fabric.hl7fhir;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public final class AssetTest extends UtilTest {
    @Nested
    class Equality {

        @Test
        public void isReflexive() {
            Asset asset = new Asset(ID_1, toValueMap(VALUE_1));
            assertThat(asset).isEqualTo(asset);
        }

        @Test
        public void isSymmetric() {
            Asset assetA = new Asset(ID_1, toValueMap(VALUE_1));
            Asset assetB = new Asset(ID_1, toValueMap(VALUE_1));
            assertThat(assetA).isEqualTo(assetB);
            assertThat(assetB).isEqualTo(assetA);
        }

        @Test
        public void isTransitive() {
            Asset assetA = new Asset(ID_1, toValueMap(VALUE_1));
            Asset assetB = new Asset(ID_1, toValueMap(VALUE_1));
            Asset assetC = new Asset(ID_1, toValueMap(VALUE_1));
            assertThat(assetA).isEqualTo(assetB);
            assertThat(assetB).isEqualTo(assetC);
            assertThat(assetA).isEqualTo(assetC);
        }

        @Test
        public void handlesInequality() {
            Asset assetA = new Asset(ID_1, toValueMap(VALUE_1));
            Asset assetB = new Asset(ID_2, toValueMap(VALUE_2));
            assertThat(assetA).isNotEqualTo(assetB);
        }

        @Test
        public void handlesOtherObjects() {
            Asset assetA = new Asset(ID_1, toValueMap(VALUE_1));
            String assetB = "not a asset";
            assertThat(assetA).isNotEqualTo(assetB);
        }

        @Test
        public void handlesNull() {
            Asset asset = new Asset(ID_1, toValueMap(VALUE_1));
            assertThat(asset).isNotEqualTo(null);
        }
    }

    @Nested
    class HashCodeAndToString {

        @Test
        void testHashCodeConsistency() {
            Asset asset1 = new Asset(ID_1, toValueMap(VALUE_1));
            Asset asset1Copy = new Asset(ID_1, toValueMap(VALUE_1)); // Equal to asset1
            Asset asset2 = new Asset(ID_2, toValueMap(VALUE_2)); // Different from asset1

            assertThat(asset1.hashCode()).isEqualTo(asset1Copy.hashCode());
            // Optional: assertThat(asset1.hashCode()).isNotEqualTo(asset2.hashCode());
            // This assertion is good practice but not strictly required by
            // Object.hashCode() contract
            // For well-distributed hash codes, different objects should ideally have
            // different hash codes.
            // Let's add it if it passes, but comment it out if it causes issues due to hash
            // collisions with simple data.
            // Given the simplicity of Asset's hashCode, collisions might be more likely
            // than with complex objects.
            // For now, we'll stick to the primary requirement: equal objects must have
            // equal hash codes.
        }

        @Test
        void testHashCodeWithNullFields() {
            // Current Asset.java calculates hash for null fields as 0.
            Asset assetWithNullId = new Asset(null, toValueMap(VALUE_1));
            Asset assetWithNullIdCopy = new Asset(null, toValueMap(VALUE_1));
            assertThat(assetWithNullId.hashCode()).isEqualTo(assetWithNullIdCopy.hashCode());

            Asset assetWithNullValue = new Asset(ID_1, null);
            Asset assetWithNullValueCopy = new Asset(ID_1, null);
            assertThat(assetWithNullValue.hashCode()).isEqualTo(assetWithNullValueCopy.hashCode());

            Asset assetWithBothNull = new Asset(null, null);
            Asset assetWithBothNullCopy = new Asset(null, null);
            assertThat(assetWithBothNull.hashCode()).isEqualTo(assetWithBothNullCopy.hashCode());

            // Test that hashCodes are different if one has null and other doesn't (where
            // objects are not equal)
            Asset assetNonNull = new Asset(ID_1, toValueMap(VALUE_1));
            assertThat(assetNonNull.hashCode()).isNotEqualTo(assetWithNullId.hashCode());
            assertThat(assetNonNull.hashCode()).isNotEqualTo(assetWithNullValue.hashCode());
        }

        @Test
        void testToString() {
            Map<String, Object> valMap = new HashMap<>();
            valMap.put("key1", "value1");
            valMap.put("key2", 123);
            Asset asset = new Asset("testAsset1", valMap);
            String expectedToString = "Asset [id=testAsset1, value=" + valMap.toString() + "]";
            assertThat(asset.toString()).isEqualTo(expectedToString);
        }
    }

    @Nested
    class GetterTests {

        @Test
        void testGetId() {
            Asset asset = new Asset("id123", toValueMap(VALUE_1));
            assertThat(asset.getId()).isEqualTo("id123");

            Asset assetWithNullId = new Asset(null, toValueMap(VALUE_1));
            assertThat(assetWithNullId.getId()).isNull();
        }

        @Test
        void testGetValue() {
            Map<String, Object> valMap = new HashMap<>();
            valMap.put("data", "sample");
            Asset asset = new Asset(ID_1, valMap);
            assertThat(asset.getValue()).isEqualTo(valMap);

            Asset assetWithNullValue = new Asset(ID_1, null);
            assertThat(assetWithNullValue.getValue()).isNull();
        }
    }
}
