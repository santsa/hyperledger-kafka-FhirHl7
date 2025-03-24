package com.kafka.consumer.extensions;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FhirMessageProcessor {

    private final Gson gson;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private ObjectMapper objectMapper;

    public FhirMessageProcessor() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public ValidationResult validateResource(IBaseResource resource) {
        try {
            log.info("\n--> Init validate Resource");
            // Ask the context for a validator
            FhirValidator validator = fhirContext.newValidator();
            // Create a validation module and register it
            IValidatorModule module = new FhirInstanceValidator(fhirContext);
            validator.registerValidatorModule(module);
            ValidationResult result = validator.validateWithResult(resource);
            if (result.isSuccessful()) {
                log.info("Validation successful. No issues found.");
            } else {
                StringBuilder outcome = new StringBuilder("Validation failed with issues:\n");
                OperationOutcome operationOutcome = (OperationOutcome) result.toOperationOutcome();
                operationOutcome.getIssue().forEach(issue -> {
                    outcome.append("Severity: ").append(issue.getSeverity()).append("\n");
                    outcome.append("Details: ").append(issue.getDiagnostics()).append("\n\n");
                });
                log.error(outcome.toString());
            }
            return result;
        } catch (Exception e) {
            log.error("\n--> Error during validation: " + e.getMessage());
            throw e;
        }
    }

    public ValidationResult validateResource(String resource) {
        try {
            log.info("\n--> Init validate Resource");
            // Ask the context for a validator
            FhirValidator validator = fhirContext.newValidator();
            // Create a validation module and register it
            IValidatorModule module = new FhirInstanceValidator(fhirContext);
            validator.registerValidatorModule(module);
            ValidationResult result = validator.validateWithResult(resource);
            if (result.isSuccessful()) {
                log.info("Validation successful. No issues found.");
            } else {
                StringBuilder outcome = new StringBuilder("Validation failed with issues:\n");
                OperationOutcome operationOutcome = (OperationOutcome) result.toOperationOutcome();
                operationOutcome.getIssue().forEach(issue -> {
                    outcome.append("Severity: ").append(issue.getSeverity()).append("\n");
                    outcome.append("Details: ").append(issue.getDiagnostics()).append("\n\n");
                });
                log.error(outcome.toString());
            }
            return result;
        } catch (Exception e) {
            log.error("\n--> Error during validation: " + e.getMessage());
            throw e;
        }
    }

    public String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }

    public String encode(final Resource resource) {
        return fhirContext.newJsonParser().encodeResourceToString(resource);
    }

    public Bundle decodeList(final String json) throws JsonProcessingException {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        JsonNode rootArray = objectMapper.readTree(json);
        for (JsonNode node : rootArray) {
            JsonNode valueNode = node.get("value");
            if (valueNode != null && !valueNode.isNull()) {
                Resource resource = parseResourceMessage(valueNode.toPrettyString()).orElse(null);
                bundle.addEntry().setResource(resource);
            } else {
                log.error("Error parsing patient message from json: " + node.get("assetID").asText());
            }
        }
        return bundle;
    }

    public Optional<Resource> decode(String json) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(json);
        return parseResourceMessage(rootNode.get("value").toPrettyString());
    }

    public Optional<Resource> parseResourceMessage(String fhirMessage) {
        Resource resource = (Resource) fhirContext.newJsonParser().parseResource(fhirMessage);
        return Optional.ofNullable(resource);
    }

}
