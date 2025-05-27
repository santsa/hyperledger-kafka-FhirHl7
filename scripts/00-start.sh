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
    ./scripts/02-start-chaincode-hl7-fhir-java.sh "1.0" "1" ""
}

function launchChaincodePrivateCollection() {
    ./scripts/02-start-chaincode-hl7-fhir-java.sh "1.0" "1" "collections_config.json"
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
    ./scripts/06-start-chaincode-hl7-fhir-java-org4.sh "1.0" "1" ""
}

function launchAddOrgChaincodePrivateCollection() {
    ./scripts/06-start-chaincode-hl7-fhir-java-org4.sh "1.0" "1" "collections_config.json"
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
    chaincode-private-collection)
        launchChaincodePrivateCollection
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
    net-chaincode-private-collection)
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincodePrivateCollection
        ;;
    net-chaincode-kafka)
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincode
        launchKafka
        ;;
    net-chaincode-kafka-private-collection)
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincodePrivateCollection
        launchKafka
        ;;
    chaincode-kafka)
        launchChaincode
        launchKafka
        ;;
    chaincode-kafka-private-collection)
        launchKafka
        launchChaincodePrivateCollection
        ;;
    chaincode-client)
        launchChaincode
        launchClient
        ;;
    chaincode-client-private-collection)
        launchClient
        launchChaincodePrivateCollection
        ;;
    chaincode-client-maven)
        launchChaincode
        launchClientMaven
        ;;
    chaincode-client-maven-private-collection)
        launchChaincodePrivateCollection
        launchClientMaven
        ;;
    addorg-all)
        launchAddOrg $2
        launchAnchorPeer "Org4" "13051" "Org4MSP"
        launchAddOrgChaincode
        ;;
    addorg-all-private-collection)
        launchAddOrg $2
        launchAnchorPeer "Org4" "13051" "Org4MSP"
        launchAddOrgChaincodePrivateCollection
        ;;
    addorg-net)
        launchAddOrg $2
        launchAnchorPeer "Org4" "13051" "Org4MSP"
        ;;
    addorg-chaincode)
        launchAddOrgChaincode
        ;;
    addorg-chaincode-private-collection)
        launchAddOrgChaincodePrivateCollection
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
    all-private-collection)
        echo "*******************************************************"
        echo "*********************Init all.sh***********************"
        echo "*******************************************************"
        launchNet $2
        launchAnchorPeer "org1" "7051" "Org1MSP"
        launchAnchorPeer "Org2" "9051" "Org2MSP"
        launchChaincodePrivateCollection
        launchClient
        ;;    
    *)
        echo "Invalid option. Use: net (couchdb or lebeldb) | chaincode | chaincode-private-collection | client | explorer | net-chaincode (couchdb or lebeldb) | net-chaincode-private-collection (couchdb or lebeldb) | net-chaincode-kafka (couchdb or lebeldb) | net-chaincode-kafka-private-collection (couchdb or lebeldb) | chaincode-kafka | chaincode-kafka-private-collection | chaincode-client | chaincode-client-private-collection | chaincode-client-maven | chaincode-client-maven-private-collection | addorg-all (couchdb or lebeldb) | addorg-all-private-collection (couchdb or lebeldb) | addorg-net (couchdb or lebeldb) | addorg-chaincode | addorg-chaincode-private-collection | all (couchdb or lebeldb) | all-private-collection (couchdb or lebeldb)"
        exit 1
        ;;
esac

echo "*******************************************************"
echo "********************End all.sh*************************"
echo "*******************************************************"
