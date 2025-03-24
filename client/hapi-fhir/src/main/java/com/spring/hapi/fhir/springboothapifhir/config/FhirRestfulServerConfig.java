package com.spring.hapi.fhir.springboothapifhir.config;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.spring.hapi.fhir.springboothapifhir.provider.PatientProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import lombok.RequiredArgsConstructor;

@WebServlet(urlPatterns = {"/baseR4/*"}, displayName = "FHIR Server")
@Configuration
@RequiredArgsConstructor
public class FhirRestfulServerConfig extends RestfulServer {

    private final ApplicationContext applicationContext;

    @Override
    protected void initialize() throws ServletException {
        super.initialize();
        setFhirContext(FhirContext.forR4Cached());
        setDefaultPrettyPrint(true);
        setResourceProviders(List.of(applicationContext.getBean(PatientProvider.class)));
    }

}
