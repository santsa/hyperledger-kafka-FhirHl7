package com.spring.hapi.fhir.springboothapifhir.config;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import ca.uhn.fhir.context.FhirContext;

@Configuration
@EnableCaching
public class Config {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4Cached();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Bean
    public Cache<String, List<? extends IBaseResource>> iBaseResourceCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, java.util.concurrent.TimeUnit.MINUTES)
                .build();
    }

}
