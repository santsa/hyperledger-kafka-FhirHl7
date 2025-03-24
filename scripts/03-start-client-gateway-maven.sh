#!/bin/bash

echo "******************************************************"
echo "***************Start launch kafka*********************"
echo "******************************************************" 
docker-compose -f network/docker-compose-client-gateway-maven.yml down -v
sleep 5
docker-compose -f network/docker-compose-client-gateway-maven.yml up -d
echo "*******************************************************"
echo "*****************End launch kafka**********************"
echo "*******************************************************"

sleep 5

cd client

export PATH="/mnt/c/Program Files/apache-maven-3.6.3/bin:$PATH"

echo "*******************************************************"
echo "*********Start launch FHIRHl7KafkaProvider*************"
echo "*******************************************************"
cd  FHIRHl7Kafka/FHIRHl7KafkaProvider
#mvn clean -Pdev spring-boot:run &
mvn clean -Pdev spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005" &
echo "*******************************************************"
echo "*********End launch FHIRHl7KafkaProvider***************"
echo "*******************************************************"

cd -
echo "*******************************************************"
echo "*********Start launch FHIRHl7KafkaConsumer*************"
echo "*******************************************************"
sleep 10
cd FHIRHl7Kafka/FHIRHl7KafkaConsumer
#mvn clean -PmvnOrg1 spring-boot:run
mvn clean -PmvnOrg1 spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5006" &
echo "*******************************************************"
echo "**********End launch FHIRHl7KafkaConsumer**************"
echo "*******************************************************"

cd -
echo "*******************************************************"
echo "***********Start launch hapi-fhir consumer*************"
echo "*******************************************************"
sleep 10
cd hapi-fhir
#mvn clean -PmvnOrg2 spring-boot:run
mvn clean -PmvnOrg2 spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5007"
echo "*******************************************************"
echo "*************End launch hapi-fhir consumer*************"
echo "*******************************************************"