FROM openjdk:21-slim
WORKDIR /app
COPY target/fhir-hl7-hapi-cache-1.0.0.jar app.jar
EXPOSE 8080
RUN chmod +x app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]