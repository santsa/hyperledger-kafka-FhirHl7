package com.kafka.consumer.extensions;

import java.util.zip.DataFormatException;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<OperationOutcome> handleDataGatewayException(GatewayException ex) {
        return new ResponseEntity<>(createOperationOutcome("Exception retrieving data: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(EndorseException.class)
    public ResponseEntity<OperationOutcome> handleDataRetrievalEndorseException(EndorseException ex) {
        return new ResponseEntity<>(createOperationOutcome("Exception endorse data: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SubmitException.class)
    public ResponseEntity<OperationOutcome> handleDataRetrievalSubmitException(SubmitException ex) {
        return new ResponseEntity<>(createOperationOutcome("Exception submit data: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CommitException.class)
    public ResponseEntity<OperationOutcome> handleDataRetrievalException(CommitException ex) {
        return new ResponseEntity<>(createOperationOutcome("Exception commit data: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(CommitStatusException.class)
    public ResponseEntity<OperationOutcome> handleDataRetrievalCommitStatusException(CommitStatusException ex) {
        return new ResponseEntity<>(createOperationOutcome("Exception commit status data: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<OperationOutcome> handleInvalidRequest(InvalidRequestException ex) {
        return new ResponseEntity<>(createOperationOutcome("Invalid request: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<OperationOutcome> handleInvalidRequest(JsonProcessingException ex) {
        return new ResponseEntity<>(createOperationOutcome("Error parsing patient message from json: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataFormatException.class)
    public ResponseEntity<OperationOutcome> handleDataFormatException(DataFormatException ex) {
        return new ResponseEntity<>(createOperationOutcome("Invalid FHIR JSON: " + ex.getMessage(), OperationOutcome.IssueSeverity.ERROR), HttpStatus.BAD_REQUEST);
    }

    private OperationOutcome createOperationOutcome(String message, OperationOutcome.IssueSeverity severity) {
        log.error(message);
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(severity).setDiagnostics(message);
        return outcome;
    }

}
