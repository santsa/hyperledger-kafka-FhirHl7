package com.kafka.consumer.service;

import java.lang.IllegalStateException;
import java.lang.UnsupportedOperationException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
public class PatientService extends FabricServiceBase {

    public void initLedger()
            throws EndorseException, SubmitException, CommitStatusException, CommitException {
        log.info("\n--> Submit Transaction: InitLedger, function creates the initial set of assets on the ledger");
        var submitResult = contract.submitTransaction("InitLedger");
        String result = processor.prettyJson(submitResult);
        log.info("*** Transaction committed successfully" + result);
    }

    public Bundle search() throws GatewayException, JsonProcessingException {
        log.info("\n--> Evaluate Transaction: GetAll, function returns all the current patients on the ledger");
        var evaluateResult = contract.evaluateTransaction("GetAllAssets", getPrivateCollectionName());
        return processor.decodeList(processor.prettyJson(evaluateResult));
    }

    public Patient searchById(String id) throws GatewayException, JsonProcessingException {
        log.info("\n--> Evaluate Transaction: readPatient, function returns patient attributes");
        var evaluateResult = contract.evaluateTransaction("ReadAsset", getPrivateCollectionName(), id);
        return (Patient) processor.decode(processor.prettyJson(evaluateResult)).get();
    }

    public boolean patientExists(String id) throws GatewayException {
        log.info("\n--> Evaluate Transaction: ReadAsset, function returns asset attributes");
        byte[] result = contract.evaluateTransaction("AssetExists", getPrivateCollectionName(), id);
        return Boolean.parseBoolean(new String(result));
    }

    public Bundle updatePatientAsync(Patient patient) throws EndorseException, SubmitException, CommitStatusException {
        log.info("\n--> Async Submit Transaction: UpdateAsset");
        String targetCollection = getPrivateCollectionName();
        if (targetCollection == null || targetCollection.isEmpty()) {
            log.error("Private collection name is not configured. Aborting operation.");
            throw new IllegalStateException("Private collection name is not configured for the current profile.");
        }
        List<String> allowedMsps = getAuthorizedMspIds();
        if (allowedMsps == null || !allowedMsps.contains(mspId)) {
            log.warn("Organization {} is not in the authorized list {} for private collection {}. Operation aborted.", mspId, allowedMsps, targetCollection);
            throw new UnsupportedOperationException("Organization " + mspId + " is not authorized for private collection " + targetCollection);
        }
        if (patient == null) {
            throw new InvalidRequestException("Patient is invalid");
        }

        Map<String, byte[]> transientMap = new HashMap<>();
        transientMap.put("asset_properties", processor.encode(patient).getBytes(StandardCharsets.UTF_8));

        var commit = contract.newProposal("UpdateAsset")
                .addArguments(getPrivateCollectionName(), patient.getIdElement().getIdPart())
                .setTransient(transientMap)
                .build().endorse().submitAsync();

        log.info("*** Waiting for transaction commit");
        var status = commit.getStatus();
        if (!status.isSuccessful()) {
            throw new RuntimeException("Transaction " + status.getTransactionId()
                    + " failed to commit with status code " + status.getCode());
        }

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
        bundle.setId(status.getTransactionId());
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(patient);
        entry.setFullUrl("Patient/" + patient.getIdElement().getIdPart());
        bundle.addEntry(entry);

        log.info("*** Transaction committed successfully " + status.isSuccessful() + "Transaction " + status.getTransactionId() + " failed to commit with status code " + status.getCode() + " Result " + commit.getResult().toString());

        return bundle;
    }

    public Bundle createOrUpdate(Patient patient) throws EndorseException, SubmitException, CommitException, CommitStatusException, Exception {
        if (patient == null) {
            throw new InvalidRequestException("Patient is invalid");
        }

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
        bundle.setId(UUID.randomUUID().toString());

        patient.setActive(true);
        patient.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());
        if (patient.getId() == null || patient.getId().isEmpty() || patient.getId().isBlank()) {
            patient.setId(new IdType("Patient", UUID.randomUUID().toString(), "1"));
            patient = createPatient(patient);
        } else if (!patientExists(patient.getIdElement().getIdPart())) {
            patient.setId(new IdType("Patient", patient.getIdElement().getIdPart(), "1"));
            patient = createPatient(patient);
        } else {
            patient.setId(new IdType("Patient", patient.getIdElement().getIdPart(), "0"));
            patient = updatePatient(patient);
        }

        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(patient);
        entry.setFullUrl("Patient/" + patient.getIdElement().getIdPart());
        bundle.addEntry(entry);
        return bundle;
    }

    private Patient createPatient(Patient patient) throws EndorseException, SubmitException, CommitException, CommitStatusException, Exception {
        log.info("\n--> Submit Transaction: createPatient, creates new patient with arguments");
        String targetCollection = getPrivateCollectionName();
        if (targetCollection == null || targetCollection.isEmpty()) {
            log.error("Private collection name is not configured. Aborting operation.");
            throw new IllegalStateException("Private collection name is not configured for the current profile.");
        }
        List<String> allowedMsps = getAuthorizedMspIds();
        if (allowedMsps == null || !allowedMsps.contains(mspId)) {
            log.warn("Organization {} is not in the authorized list {} for private collection {}. Operation aborted.", mspId, allowedMsps, targetCollection);
            throw new UnsupportedOperationException("Organization " + mspId + " is not authorized for private collection " + targetCollection);
        }
        Map<String, byte[]> transientMap = new HashMap<>();
        transientMap.put("asset_properties", processor.encode(patient).getBytes(StandardCharsets.UTF_8));
        var submitResult = contract.newProposal("CreateAsset").addArguments(getPrivateCollectionName()).setTransient(transientMap).build().endorse().submit();
        return (Patient) processor.decode(processor.prettyJson(submitResult)).get();
    }

    private Patient updatePatient(Patient patient) throws EndorseException, SubmitException, CommitException, CommitStatusException, Exception {
        log.info("\n--> Submit Transaction: UpdatePatient");
        String targetCollection = getPrivateCollectionName();
        if (targetCollection == null || targetCollection.isEmpty()) {
            log.error("Private collection name is not configured. Aborting operation.");
            throw new IllegalStateException("Private collection name is not configured for the current profile.");
        }
        List<String> allowedMsps = getAuthorizedMspIds();
        if (allowedMsps == null || !allowedMsps.contains(mspId)) {
            log.warn("Organization {} is not in the authorized list {} for private collection {}. Operation aborted.", mspId, allowedMsps, targetCollection);
            throw new UnsupportedOperationException("Organization " + mspId + " is not authorized for private collection " + targetCollection);
        }
        Map<String, byte[]> transientMap = new HashMap<>();
        transientMap.put("asset_properties", processor.encode(patient).getBytes(StandardCharsets.UTF_8));
        var submitResult = contract.newProposal("UpdateAsset").addArguments(getPrivateCollectionName()).setTransient(transientMap).build().endorse().submit();
        return (Patient) processor.decode(processor.prettyJson(submitResult)).get();
    }

    public Bundle deletePatient(String id) throws EndorseException, SubmitException, CommitException, CommitStatusException, JsonProcessingException {
        log.info("\n--> Submit Transaction: deletePatient " + id);
        var submitResult = contract.submitTransaction("DeleteAsset", getPrivateCollectionName(), id);
        Patient patient = (Patient) processor.decode(processor.prettyJson(submitResult)).get();

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
        bundle.setId(UUID.randomUUID().toString());

        OperationOutcome opOutcome = new OperationOutcome();
        opOutcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setCode(OperationOutcome.IssueType.DELETED)
                .setDiagnostics("Patient successfully deleted.");

        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(patient);
        entry.setFullUrl("Patient/" + patient.getIdElement().getIdPart());
        bundle.addEntry(entry);
        return bundle;
    }

}
