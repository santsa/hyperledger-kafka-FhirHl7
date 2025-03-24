package com.kafka.consumer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hyperledger.fabric.client.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kafka.consumer.extensions.FhirMessageProcessor;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private Contract contract;

    @Mock
    private FhirMessageProcessor processor;

    @InjectMocks
    private PatientService patientService;

    private static final String SAMPLE_JSON = "[{\"resourceType\":\"Patient\",\"id\":\"123\",\"active\":true}]";
    private static final byte[] SAMPLE_BYTES = SAMPLE_JSON.getBytes();
    private static final String SINGLE_PATIENT_JSON = "{\"value\":{\"resourceType\":\"Patient\",\"id\":\"123\",\"active\":true}}";
    private static final byte[] SINGLE_PATIENT_BYTES = SINGLE_PATIENT_JSON.getBytes();

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void search_ShouldReturnBundleWithPatients() throws Exception {
        // Arrange
        Bundle expectedBundle = new Bundle();
        Patient patient = new Patient();
        patient.setId("123");
        patient.setActive(true);
        expectedBundle.addEntry().setResource(patient);

        when(contract.evaluateTransaction("GetAllAssets")).thenReturn(SAMPLE_BYTES);
        when(processor.prettyJson(SAMPLE_BYTES)).thenReturn(SAMPLE_JSON);
        when(processor.decodeList(SAMPLE_JSON)).thenReturn(expectedBundle);

        // Act
        Bundle result = patientService.search();

        // Assert
        assertNotNull(result, "Bundle should not be null");
        assertEquals(1, result.getEntry().size(), "Bundle should contain one patient");
        Patient resultPatient = (Patient) result.getEntryFirstRep().getResource();
        assertEquals("123", resultPatient.getId(), "Patient ID should match");
        assertEquals(true, resultPatient.getActive(), "Patient should be active");
    }

    @Test
    void searchById_ShouldReturnPatient() throws Exception {
        // Arrange
        String patientId = "123";
        Patient expectedPatient = new Patient();
        expectedPatient.setId(patientId);
        expectedPatient.setActive(true);

        when(contract.evaluateTransaction("ReadAsset", patientId)).thenReturn(SINGLE_PATIENT_BYTES);
        when(processor.prettyJson(SINGLE_PATIENT_BYTES)).thenReturn(SINGLE_PATIENT_JSON);
        when(processor.decode(SINGLE_PATIENT_JSON)).thenReturn(Optional.of(expectedPatient));

        Patient result = patientService.searchById(patientId);

        assertNotNull(result, "Patient should not be null");
        assertEquals(patientId, result.getId(), "Patient ID should match");
        assertEquals(true, result.getActive(), "Patient should be active");
    }

    @Test
    void patientExists_ShouldReturnTrue_WhenPatientExists() throws Exception {
        String patientId = "123";
        when(contract.evaluateTransaction("AssetExists", patientId)).thenReturn("true".getBytes());
        boolean result = patientService.patientExists(patientId);
        assertTrue(result, "Patient should exist");
    }

    @Test
    void patientExists_ShouldReturnFalse_WhenPatientDoesNotExist() throws Exception {
        String patientId = "456";
        when(contract.evaluateTransaction("AssetExists", patientId)).thenReturn("false".getBytes());
        boolean result = patientService.patientExists(patientId);
        assertFalse(result, "Patient should not exist");
    }

    @Test
    void createOrUpdate_ShouldCreateNewPatient_WhenNoIdProvided() throws Exception {

        Patient newPatient = new Patient();
        String expectedEncodedPatient = "{\"resourceType\":\"Patient\",\"active\":true}";

        when(processor.encode(any(Patient.class))).thenReturn(expectedEncodedPatient);
        when(contract.submitTransaction("CreateAsset", expectedEncodedPatient))
            .thenReturn(SINGLE_PATIENT_BYTES);
        when(processor.prettyJson(SINGLE_PATIENT_BYTES)).thenReturn(SINGLE_PATIENT_JSON);
        when(processor.decode(SINGLE_PATIENT_JSON)).thenReturn(Optional.of(newPatient));
        Bundle result = patientService.createOrUpdate(newPatient);

        // Assert
        assertNotNull(result, "Bundle should not be null");
        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, result.getType(), "Bundle type should be TRANSACTIONRESPONSE");
        assertNotNull(result.getId(), "Bundle ID should not be null");

        Patient createdPatient = (Patient) result.getEntryFirstRep().getResource();
        assertNotNull(createdPatient, "Created patient should not be null");
        assertTrue(createdPatient.getActive(), "Patient should be active");
        assertNotNull(createdPatient.getId(), "Patient should have an ID");
        assertTrue(createdPatient.getId().contains("Patient/"), "Patient ID should be properly formatted");
        assertNotNull(createdPatient.getMeta().getLastUpdated(), "Last updated should be set");
    }

    @Test
    void deletePatient_ShouldReturnBundleWithDeletedPatient() throws Exception {

        String patientId = "123";
        Patient deletedPatient = new Patient();
        deletedPatient.setId(patientId);
        deletedPatient.setActive(false);

        when(contract.submitTransaction("DeleteAsset", patientId)).thenReturn(SINGLE_PATIENT_BYTES);
        when(processor.prettyJson(SINGLE_PATIENT_BYTES)).thenReturn(SINGLE_PATIENT_JSON);
        when(processor.decode(SINGLE_PATIENT_JSON)).thenReturn(Optional.of(deletedPatient));

        Bundle result = patientService.deletePatient(patientId);

        assertNotNull(result, "Bundle should not be null");
        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, result.getType(), "Bundle type should be TRANSACTIONRESPONSE");
        assertNotNull(result.getId(), "Bundle ID should not be null");

        Patient resultPatient = (Patient) result.getEntryFirstRep().getResource();
        assertNotNull(resultPatient, "Deleted patient should be included in response");
        assertEquals(patientId, resultPatient.getIdElement().getIdPart(), "Patient ID should match");
        assertEquals(deletedPatient.getActive(), resultPatient.getActive(), "Patient should be inactive");
    }
}