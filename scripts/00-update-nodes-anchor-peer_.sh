#!/bin/bash

export ORG="$1"
export PORT="$2"
export PATH=${PWD}/bin:${PWD}:$PATH
export FABRIC_CFG_PATH=${PWD}/config
export ORDERER_CA=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem
export ORDERER_ADMIN_TLS_SIGN_CERT=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.crt
export ORDERER_ADMIN_TLS_PRIVATE_KEY=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.key
export CORE_PEER_TLS_ENABLED=true

echo "*******************************************************"
echo "****************Init anchor peer ${ORG}****************"
echo "*******************************************************"

# Identify the organization
export CORE_PEER_LOCALMSPID="$3"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/${ORG,,}.hl7-fhir.com/peers/peer0.${ORG,,}.hl7-fhir.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/${ORG,,}.hl7-fhir.com/users/Admin@${ORG,,}.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:${PORT}

# Get the last config of the channel
peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com -c hannelhl7fhir --tls --cafile "$ORDERER_CA"

echo "Get the last config of the channel"

# Extract and modify the configuration
cd channel-artifacts
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
echo "1"
jq '.data.data[0].payload.data.config' config_block.json > config.json
echo "2"
cp config.json config_copy.json
echo "3"
jq ".channel_group.groups.Application.groups.${ORG}MSP.values += {\"AnchorPeers\":{\"mod_policy\": \"Admins\",\"value\":{\"anchor_peers\": [{\"host\": \"peer0.${ORG,,}.hl7-fhir.com\",\"port\": ${PORT}}]},\"version\": \"0\"}}" config_copy.json > modified_config.json
echo "4"
configtxlator proto_encode --input config.json --type common.Config --output config.pb
echo "5"
configtxlator proto_encode --input modified_config.json --type common.Config --output modified_config.pb
echo "6"
configtxlator compute_update --channel_id hannelhl7fhir --original config.pb --updated modified_config.pb --output config_update.pb
echo "7"
configtxlator proto_decode --input config_update.pb --type common.ConfigUpdate --output config_update.json
echo "8"
echo '{"payload":{"header":{"channel_header":{"channel_id":"hannelhl7fhir", "type":2}},"data":{"config_update":'$(cat config_update.json)'}}}' | jq . >config_update_in_envelope.json
configtxlator proto_encode --input config_update_in_envelope.json --type common.Envelope --output config_update_in_envelope.pb
echo "9"
cd ..

# Update the channel configuration
peer channel update -f channel-artifacts/config_update_in_envelope.pb -c hannelhl7fhir -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com --tls --cafile "$ORDERER_CA"

echo "*******************************************************"
echo "*****************End anchor peer ${ORG}*****************"
echo "*******************************************************"
