#!/bin/bash
echo "*******************************************************"
echo "******************Init 03-start-client******************"
echo "*******************************************************"

export PATH="/mnt/c/Program Files/apache-maven-3.6.3/bin:$PATH"

echo "******************************************************"
echo "***************Start maven provider********************"
echo "******************************************************"
cd  client/FHIRHl7Kafka/FHIRHl7KafkaProvider
mvn clean package -Pdocker
echo "******************************************************"
echo "****************End maven provider*********************"
echo "******************************************************"
cd -

echo "******************************************************"
echo "***************Start maven consumer********************"
echo "******************************************************"
cd  client/FHIRHl7Kafka/FHIRHl7KafkaConsumer
mvn clean package -PdockerOrg1
echo "******************************************************"
echo "****************End maven consumer*********************"
echo "******************************************************"
cd -

echo "******************************************************"
echo "***************Start maven hapi-fhir********************"
echo "******************************************************"
cd  client/hapi-fhir
mvn clean package -PdockerOrg2
cd -
echo "******************************************************"
echo "****************End maven package*********************"
echo "******************************************************"

echo "******************************************************"
echo "***************Start launch kafka*********************"
echo "******************************************************"
docker-compose -f network/docker-compose-client-gateway.yml down -v
sleep 5
docker-compose -f network/docker-compose-client-gateway.yml up --build -d
echo "*******************************************************"
echo "*****************End launch kafka**********************"
echo "*******************************************************"
