#!/bin/bash
#sudo -u sgorrita ./scripts/01-start-net.sh

echo "*******************************************************"
echo "******************Init 01-start-net********************"
echo "*******************************************************"

export PATH_HOME=${PWD}

rm -rf organizations/peerOrganizations
rm -rf organizations/ordererOrganizations
rm -rf channel-artifacts/
mkdir channel-artifacts
rm system-genesis-block/genesis.block

export PATH=$PATH_HOME/bin:$PATH_HOME:$PATH
export FABRIC_CFG_PATH=$PATH_HOME/config
echo "*******************************************************"
echo "*******generate certificates in this new net***********"
echo "*******************************************************"
cryptogen generate --config=./organizations/cryptogen/crypto-config-org1.yaml --output="organizations"
cryptogen generate --config=./organizations/cryptogen/crypto-config-org2.yaml --output="organizations"
cryptogen generate --config=./organizations/cryptogen/crypto-config-orderer.yaml --output="organizations"

echo "*******************************************************"
echo "***************copy connections gateway****************"
echo "*******************************************************"
cp ./organizations/cryptogen/connections-templates/connection-devOrg1.json ./organizations/peerOrganizations/org1.hl7-fhir.com/connection-devOrg1.json
cp ./organizations/cryptogen/connections-templates/connection-dockerOrg1.json ./organizations/peerOrganizations/org1.hl7-fhir.com/connection-dockerOrg1.json
cp ./organizations/cryptogen/connections-templates/connection-mvnOrg1.json ./organizations/peerOrganizations/org1.hl7-fhir.com/connection-mvnOrg1.json
cp ./organizations/cryptogen/connections-templates/connection-devOrg2.json ./organizations/peerOrganizations/org2.hl7-fhir.com/connection-devOrg2.json
cp ./organizations/cryptogen/connections-templates/connection-dockerOrg2.json ./organizations/peerOrganizations/org2.hl7-fhir.com/connection-dockerOrg2.json
cp ./organizations/cryptogen/connections-templates/connection-mvnOrg2.json ./organizations/peerOrganizations/org2.hl7-fhir.com/connection-mvnOrg2.json

echo "*******************************************************"
echo "*******************launch the net**********************"
echo "*******************************************************"

case "$1" in
    couchdb)
        echo "*************************couchdb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-couchdb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-couchdb.yaml up -d
        ;;
    lebeldb)
        echo "*************************lebeldb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-lebeldb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-lebeldb.yaml up -d
        ;;

    *)
        echo "*************************couchdb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-couchdb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-couchdb.yaml up -d
        ;;
esac

sleep 5
echo "*******************************************************"
echo "*******************create channel**********************"
echo "*******************************************************"
export FABRIC_CFG_PATH=$PATH_HOME/configtx
configtxgen -profile TwoOrgsApplicationGenesis -outputBlock ./channel-artifacts/channelhl7fhir.block -channelID channelhl7fhir

sleep 5
echo "*******************************************************"
echo "*******************add orderer*************************"
echo "*******************************************************"
export FABRIC_CFG_PATH=$PATH_HOME/config
export ORDERER_CA=$PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem
export ORDERER_ADMIN_TLS_SIGN_CERT=$PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.crt
export ORDERER_ADMIN_TLS_PRIVATE_KEY=$PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.key
osnadmin channel join --channelID channelhl7fhir --config-block ./channel-artifacts/channelhl7fhir.block -o localhost:7053 --ca-file "$ORDERER_CA" --client-cert "$ORDERER_ADMIN_TLS_SIGN_CERT" --client-key "$ORDERER_ADMIN_TLS_PRIVATE_KEY"

osnadmin channel list -o localhost:7053 --ca-file "$ORDERER_CA" --client-cert "$ORDERER_ADMIN_TLS_SIGN_CERT" --client-key "$ORDERER_ADMIN_TLS_PRIVATE_KEY"

sleep 5
echo "*******************************************************"
echo "*******************add node 1**************************"
echo "*******************************************************"
export CORE_PEER_TLS_ENABLED=true
export PEER0_ORG1_CA=$PATH_HOME/organizations/peerOrganizations/org1.hl7-fhir.com/peers/peer0.org1.hl7-fhir.com/tls/ca.crt
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG1_CA
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org1.hl7-fhir.com/users/Admin@org1.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:7051
peer channel join -b ./channel-artifacts/channelhl7fhir.block
peer channel list

sleep 5
echo "*******************************************************"
echo "*******************add node 2**************************"
echo "*******************************************************"
export PEER0_ORG2_CA=$PATH_HOME/organizations/peerOrganizations/org2.hl7-fhir.com/peers/peer0.org2.hl7-fhir.com/tls/ca.crt
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG2_CA
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org2.hl7-fhir.com/users/Admin@org2.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:9051
peer channel join -b ./channel-artifacts/channelhl7fhir.block
peer channel list

peer channel getinfo -c channelhl7fhir

echo "*******************************************************"
echo "******************End 01-start-net*********************"
echo "*******************************************************"