FROM openjdk:17-jdk-slim
WORKDIR /app
COPY FHIRHl7KafkaConsumer/target/FHIRHl7KafkaConsumer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
RUN chmod +x app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
