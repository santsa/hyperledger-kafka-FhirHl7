#!/bin/bash
#git clone https://github.com/salva/pipeline-hyperledger-fhir-hl7.git
#cd pipeline-hyperledger-fhir-hl7
#sudo -u sgorrita ./scripts/00-start.sh all couchdb
#sudo -u sgorrita ./scripts/00-start.sh all lebeldb

function launchNet() {
    ./scripts/01-start-net.sh "$1"
}

function launchChaincode() {
    ./scripts/02-start-chaincode-hl7-fhir-java.sh
}

function launchClient() {
    ./scripts/03-start-client-gateway.sh
}

function launchClientMaven() {
    ./scripts/03-start-client-gateway-maven.sh
}

function launchExplorer() {
    ./scripts/04-start-hyperledger-explorer.sh
}

function launchKafka(){
    ./scripts/03-start-only-kafka.sh
}

# Check if a parameter was passed
case "$1" in
    net)
        launchNet $2
        ;;
    chaincode)
        launchChaincode
        ;;
    client)
        launchClient
        ;;
    kafka)
        launchKafka
        ;;
    client-maven)
        launchClientMaven
        ;;
    explorer)
        launchExplorer
        ;;
    net-chaincode)
        launchNet $2
        launchChaincode
        ;;
    net-chaincode-kafka)
        launchNet $2
        launchChaincode
        launchKafka
        ;;
    chaincode-kafka)
        launchChaincode
        launchKafka
        ;;
    chaincode-client)
        launchChaincode
        launchClient
        ;;
    chaincode-client-maven)
        launchChaincode
        launchClientMaven
        ;;
    all)
        echo "*******************************************************"
        echo "*********************Init all.sh***********************"
        echo "*******************************************************"
        launchNet $2
        launchChaincode
        launchClient
        ;;
    *)
        echo "Invalid option. Use: net (couchdb or lebeldb) | chaincode | client | explorer | net-chaincode (couchdb or lebeldb) | chaincode-client | all (couchdb or lebeldb)"
        exit 1
        ;;
esac

echo "*******************************************************"
echo "********************End all.sh*************************"
echo "*******************************************************"
