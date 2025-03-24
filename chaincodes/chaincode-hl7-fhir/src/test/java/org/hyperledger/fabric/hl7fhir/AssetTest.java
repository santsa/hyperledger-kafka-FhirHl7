package org.hyperledger.fabric.hl7fhir;

import static org.assertj.core.api.Assertions.assertThat;

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
}
