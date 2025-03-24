package com.kafka.consumer.listener;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import com.kafka.consumer.service.PatientService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaConsumerListener {

    @Autowired
    private PatientService patientService;

    @KafkaListener(topics = "FHIR-messages-patient", groupId = "fhir-hl7-consumers")
    public void listenerFHIR(String message) {
        log.info("Received message: " + message);
        try {
            if (patientService.getProcessor().validateResource(message).isSuccessful()) {
                patientService.createOrUpdate((Patient) patientService.getProcessor().parseResourceMessage(message).get());
            } else {
                log.error("Error processing FHIR message: " + message);
            }
        } catch (Exception e) {
            log.error("Error processing FHIR message: " + e.getMessage());
        }
    }

    /*@KafkaListener(topics = "HL7-messages-patient", groupId = "fhir-hl7-consumers")
    public void listenerHL7(String message) {
        log.info("Received message: " + message);
    }*/
}
