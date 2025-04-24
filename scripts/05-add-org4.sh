#!/bin/bash
#sudo -u sgorrita ./scripts/00-start.sh addorg-net lebeldb
echo "*******************************************************"
echo "******************Init 05-add-org4*********************"
echo "*******************************************************"
export PATH=${PWD}/bin:${PWD}:$PATH
export FABRIC_CFG_PATH=${PWD}/config

echo "*******************************************************"
echo "*************generate certificates org4****************"
echo "*******************************************************"

rm -rf ./organizations/peerOrganizations/org4.hl7-fhir.com
rm ./channel-artifacts/*.json
rm ./channel-artifacts/*.pb

cryptogen generate --config=./organizations/cryptogen/crypto-config-org4.yaml --output="organizations"

echo "*******************************************************"
echo "*************generate configuraton org4****************"
echo "*******************************************************"
cd configtx-org4/
export FABRIC_CFG_PATH=$PWD
echo $FABRIC_CFG_PATH
../bin/configtxgen -printOrg Org4MSP > ../organizations/peerOrganizations/org4.hl7-fhir.com/org4.json
cd -
sleep 5

case "$1" in
    couchdb)
        echo "*************************couchdb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-org4-couchdb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-org4-couchdb.yaml up -d
        ;;
    lebeldb)
        echo "*************************lebeldb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-org4-lebeldb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-org4-lebeldb.yaml up -d
        ;;

    *)
        echo "*************************couchdb***********************"
        echo "*******************************************************"
        docker-compose -f network/docker-compose-pipeline-org4-couchdb.yaml down -v
        sleep 10
        docker-compose -f network/docker-compose-pipeline-org4-couchdb.yaml up -d
        ;;
esac

sleep 5
echo "*******************************************************"
echo "**************update from org1 to org4*****************"
echo "*******************************************************"

export PATH=${PWD}/bin:$PATH
export FABRIC_CFG_PATH=${PWD}/config
export CORE_PEER_TLS_ENABLED=true
export PEER0_ORG1_CA=${PWD}/organizations/peerOrganizations/org1.hl7-fhir.com/peers/peer0.org1.hl7-fhir.com/tls/ca.crt

export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG1_CA
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.hl7-fhir.com/users/Admin@org1.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:7051
peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com -c channelhl7fhir --tls --cafile ${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

sleep 5

echo "*******************************************************"
echo "***************decrypt in json format******************"
echo "*******************************************************"
cd channel-artifacts
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
jq .data.data[0].payload.data.config config_block.json > config.json

echo "*******************************************************"
echo "*add configuration config.json to modified_config.json*"
echo "*******************************************************"
jq -s '.[0] * {"channel_group":{"groups":{"Application":{"groups": {"Org4MSP":.[1]}}}}}' config.json ../organizations/peerOrganizations/org4.hl7-fhir.com/org4.json > modified_config.json
configtxlator proto_encode --input config.json --type common.Config --output config.pb
configtxlator proto_encode --input modified_config.json --type common.Config --output modified_config.pb
configtxlator compute_update --channel_id channelhl7fhir --original config.pb --updated modified_config.pb --output org4_update.pb
configtxlator proto_decode --input org4_update.pb --type common.ConfigUpdate --output org4_update.json
echo '{"payload":{"header":{"channel_header":{"channel_id":"'channelhl7fhir'", "type":2}},"data":{"config_update":'$(cat org4_update.json)'}}}' | jq . > org4_update_in_envelope.json
configtxlator proto_encode --input org4_update_in_envelope.json --type common.Envelope --output org4_update_in_envelope.pb

sleep 5
echo "*******************************************************"
echo "*majority sign of users admins to acept for the policy*"
echo "*******************************************************"
cd ..
peer channel signconfigtx -f channel-artifacts/org4_update_in_envelope.pb

echo "*******************************************************"
echo "**************update from org2 to org4*****************"
echo "*******************************************************"
export PEER0_ORG2_CA=${PWD}/organizations/peerOrganizations/org2.hl7-fhir.com/peers/peer0.org2.hl7-fhir.com/tls/ca.crt
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG2_CA
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org2.hl7-fhir.com/users/Admin@org2.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:9051
peer channel update -f channel-artifacts/org4_update_in_envelope.pb -c channelhl7fhir -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com --tls --cafile ${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

sleep 5

echo "*******************************************************"
echo "******************launch node update*******************"
echo "*******************************************************"
export PEER0_ORG4_CA=${PWD}/organizations/peerOrganizations/org4.hl7-fhir.com/peers/peer0.org4.hl7-fhir.com/tls/ca.crt
export CORE_PEER_LOCALMSPID="Org4MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG4_CA
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org4.hl7-fhir.com/users/Admin@org4.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:13051
peer channel fetch 0 channel-artifacts/channelhl7fhir.block -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com -c channelhl7fhir --tls --cafile ${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

sleep 5
echo "*****************************************************************************"
echo "**last configuration of channel has updates and now can join to the channel**"
echo "*****************************************************************************"
peer channel join -b channel-artifacts/channelhl7fhir.block


echo "*******************************************************"
echo "*******************End 05-add-org4*********************"
echo "*******************************************************"
