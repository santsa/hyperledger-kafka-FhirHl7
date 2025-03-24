package org.hyperledger.fabric.hl7fhir;

import java.util.Map;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Asset {

    @Property()
    private final String id;

    @Property()
    private Map<String, Object> value;

    public String getId() {
        return id;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public Asset(@JsonProperty("id") final String id, @JsonProperty("value") final Map<String, Object> value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Asset other = (Asset) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else {
            if (!id.equals(other.id)) {
                return false;
            }
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else {
            if (!value.equals(other.value)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Asset [id=" + id + ", value=" + value.toString() + "]";
    }

}
