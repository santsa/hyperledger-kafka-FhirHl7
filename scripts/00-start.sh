#!/bin/bash
#git clone https://github.com/salva/hyperledger-kafka-FhirHl7.git
#cd hyperledger-kafka-FhirHl7
#sudo -u sgorrita ./scripts/00-start.sh all couchdb
#sudo -u sgorrita ./scripts/00-start.sh all lebeldb

function launchNet() {
    ./scripts/01-start-net.sh "$1"
}

function launchAnchorPeer() {
    ./scripts/00-update-nodes-anchor-peer.sh "$1" "$2" "$3"
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

function launchAddOrg() {
    ./scripts/05-add-org4.sh "$1"
}

function launchAddOrgChaincode() {
    ./scripts/06-start-chaincode-hl7-fhir-java-org4.sh
}

# Check if a parameter was passed
case "$1" in
    net)
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
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
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincode
        ;;
    net-chaincode-kafka)
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
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
    addorg-all)
        launchAddOrg $2
        launchAnchorPeer "Org4" "13051" "Org4MSP"
        launchAddOrgChaincode
        ;;
    addorg-net)
        launchAddOrg $2
        launchAnchorPeer "Org4" "13051" "Org4MSP"
        ;;
    addorg-chaincode)
        launchAddOrgChaincode
        ;;
    all)
        echo "*******************************************************"
        echo "*********************Init all.sh***********************"
        echo "*******************************************************"
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincode
        launchClient
        ;;
    *)
        echo "Invalid option. Use: net (couchdb or lebeldb) | chaincode | client | explorer | net-chaincode (couchdb or lebeldb) | chaincode-client | addorg-all (couchdb or lebeldb) | addorg-net (couchdb or lebeldb) | addorg-chaincode | all (couchdb or lebeldb)"
        exit 1
        ;;
esac

echo "*******************************************************"
echo "********************End all.sh*************************"
echo "*******************************************************"
