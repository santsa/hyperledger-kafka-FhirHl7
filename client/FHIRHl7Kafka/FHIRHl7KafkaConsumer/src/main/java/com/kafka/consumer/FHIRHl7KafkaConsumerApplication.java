package com.kafka.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FHIRHl7KafkaConsumerApplication {
	//mvn -PmvnOrg1 spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5006"
	public static void main(String[] args) {
		SpringApplication.run(FHIRHl7KafkaConsumerApplication.class, args);
	}

}
