package com.spring.hapi.fhir.springboothapifhir.provider;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hyperledger.fabric.gateway.ContractException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Component
public class PatientProvider extends ResourceProviderBase implements IResourceProvider {

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    @Search
    public List<Resource> getAllPatients() throws ContractException, JsonProcessingException {
        var evaluateResult = contract.evaluateTransaction("GetAllAssets");
        List<Resource> resources = decodeList(prettyJson(evaluateResult));
        if (resources.isEmpty()) {
            throw new ResourceNotFoundException("No patients found");
        }
        return resources.stream()
                .filter(patient -> patient instanceof Patient && ((Patient) patient).getActive())
                .collect(Collectors.toList());
        /*return config.iBaseResourceCache().asMap().values().stream()
                .map(list -> list.isEmpty() ? null : list.get(list.size() - 1))
                .filter(Objects::nonNull)
                .filter(patient -> patient instanceof Patient && ((Patient) patient).getActive())
                .map(patient -> (Patient) patient)
                .collect(Collectors.toList());*/
    }

    private Patient getPatientById(IdType theId) throws ContractException, JsonProcessingException {
        var evaluateResult = contract.evaluateTransaction("ReadAsset", theId.getIdPart());
        return (Patient) decode(prettyJson(evaluateResult)).get();
    }

    private Patient getPatientActive(IdType theId) throws JsonProcessingException, ContractException {
        Patient currentPatient = (Patient) getResource(theId);
        if (!currentPatient.getActive()) {
            throw new ResourceNotFoundException("Patient " + theId.getIdPart() + " deleted");
        }
        return currentPatient;
    }

    //http://localhost:8082/hapi-fhir/baseR4/Patient/123
    //http://localhost:8082/hapi-fhir/baseR4/Patient/123/_history/2
    @Read(version = true)
    public Patient readOrVread(@IdParam IdType theId) throws JsonProcessingException, ContractException {
        return getPatientActive(theId);
    }

    @History()
    public List<? extends IBaseResource> getPatientHistory(@IdParam IdType theId) throws JsonProcessingException, ContractException {
        return getResourceAll(theId).stream()
                .filter(resource -> resource instanceof Patient)
                .map(resource -> (Patient) resource)
                .collect(Collectors.toList());
    }

    @Update
    public MethodOutcome updatePatient(@ResourceParam Patient thePatient, @IdParam IdType theId) throws JsonProcessingException, ContractException, TimeoutException, InterruptedException {

        Patient currentPatient = getPatientActive(theId);

        int version = Integer.parseInt(currentPatient.getIdElement().getVersionIdPart()) + 1;
        thePatient.setId(new IdType("Patient", theId.getIdPart(), version + ""));
        thePatient.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());
        thePatient.setActive(true);

        var submitResult = contract.submitTransaction("UpdateAsset", encode(thePatient));
        thePatient = (Patient) decode(prettyJson(submitResult)).get();

        updateCache(thePatient);

        MethodOutcome retVal = getMethodOutcome(thePatient);
        return retVal;
    }

    @Delete()
    public MethodOutcome deletePatient(@IdParam IdType theId) throws JsonProcessingException, ContractException, TimeoutException, InterruptedException {
        Patient thePatient = getPatientActive(theId);
        thePatient.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());
        thePatient.setActive(false);

        var submitResult = contract.submitTransaction("DeleteAsset", theId.getIdPart());
        Patient patient = (Patient) decode(prettyJson(submitResult)).get();

        deleteCache(patient);

        MethodOutcome retVal = getMethodOutcome(patient);
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setCode(OperationOutcome.IssueType.DELETED)
                .setDiagnostics("Patient successfully deleted.");
        retVal.setOperationOutcome(outcome);
        return retVal;
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) throws ContractException, TimeoutException, InterruptedException, JsonProcessingException {
        IdType theId = new IdType("Patient", UUID.randomUUID().toString(), "1");
        thePatient.setId(theId);
        thePatient.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());
        thePatient.setActive(true);

        var submitResult = contract.submitTransaction("CreateAsset", encode(thePatient));
        thePatient = (Patient) decode(prettyJson(submitResult)).get();

        addCache(thePatient);

        MethodOutcome retVal = getMethodOutcome(thePatient);
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setDiagnostics("Patient successfully created.");
        retVal.setOperationOutcome(outcome);
        return retVal;
    }

}
