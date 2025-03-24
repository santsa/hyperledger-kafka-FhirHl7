package com.spring.hapi.fhir.springboothapifhir;

import org.springframework.beans.factory.annotation.Value;

import com.spring.hapi.fhir.springboothapifhir.config.FhirRestfulServerConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootHapiFhirApplication
{

  @Value("${path.prefix}")
  private String pathPrefix;

  @Value("${spring.application.name}")
  private String springApplicationName;

  public static void main(String[] args)
  {
    SpringApplication.run(SpringBootHapiFhirApplication.class, args);
  }

  @Bean
  public ServletRegistrationBean<FhirRestfulServerConfig> servletRegistrationBean(ApplicationContext context)
  {
    ServletRegistrationBean<FhirRestfulServerConfig> registration = new ServletRegistrationBean<>(new FhirRestfulServerConfig(context), pathPrefix + "/*");
    registration.setName(springApplicationName);
    return registration;
  }

}
