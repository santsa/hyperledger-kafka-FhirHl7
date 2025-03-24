package com.kafka.consumer.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FhirJsonMessageConverter extends AbstractHttpMessageConverter<Resource> {

    private final FhirContext fhirContext;

    private final List<Class<? extends Resource>> supportedTypes;

    public FhirJsonMessageConverter() {
        super(MediaType.valueOf("application/fhir+json"), MediaType.APPLICATION_JSON);
        this.supportedTypes = Arrays.asList(
                Resource.class
        );
        this.fhirContext = FhirContext.forR4();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Resource.class.isAssignableFrom(clazz)
                && supportedTypes.stream().anyMatch(type -> type.isAssignableFrom(clazz));
    }

    @Override
    protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        String json = new BufferedReader(new InputStreamReader(inputMessage.getBody()))
                .lines().collect(Collectors.joining("\n"));
        return ((Resource) fhirContext.newJsonParser().parseResource(clazz, json));
    }

    @Override
    protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        fhirContext.newJsonParser().encodeResourceToWriter(resource, new OutputStreamWriter(outputMessage.getBody()));
    }
}
