package org.hyperledger.fabric.hl7fhir;

import java.util.Map;

import com.owlike.genson.Genson;

abstract class UtilTest {

    private final Genson genson = new Genson();

    public static final String ID_1 = "asset1";
    public static final String ID_2 = "asset2";
    public static final String ID_3 = "asset3";
    public static final String ID_4 = "asset4";
    public static final String ID_5 = "asset5";
    public static final String ID_6 = "asset6";
    // CHECKSTYLE:OFF
    public static final String VALUE_1 = "{\r\n  \"id\": \"asset1\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_2 = "{\r\n  \"id\": \"asset2\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_3 = "{\r\n  \"id\": \"asset3\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_4 = "{\r\n  \"id\": \"asset4\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_5 = "{\r\n  \"id\": \"asset5\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    public static final String VALUE_6 = "{\r\n  \"id\": \"asset6\",\r\n  \"resourceType\": \"Patient\",\r\n  \"data\": {\r\n    \"name\": [\r\n      {\r\n        \"use\": \"official\",\r\n        \"family\": \"Smith\",\r\n        \"given\": [\"John\"]\r\n      }\r\n    ],\r\n    \"gender\": \"male\",\r\n    \"birthDate\": \"1985-05-23\"\r\n  }\r\n}";
    // CHECKSTYLE:ON

    @SuppressWarnings("unchecked")
    public Map<String, Object> toValueMap(final String value) {
        return genson.deserialize(value, Map.class);
    }

}
