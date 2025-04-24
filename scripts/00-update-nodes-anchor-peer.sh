#!/bin/bash
export ORG="$1"
export PORT="$2"
echo "*******************************************************"
echo "****************Init anchor peer ${ORG}****************"
echo "*******************************************************"

export PATH=${PWD}/bin:${PWD}:$PATH
export FABRIC_CFG_PATH=${PWD}/config
export ORDERER_CA=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem
export ORDERER_ADMIN_TLS_SIGN_CERT=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.crt
export ORDERER_ADMIN_TLS_PRIVATE_KEY=${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/tls/server.key
export CORE_PEER_TLS_ENABLED=true

#identify like org1
export CORE_PEER_LOCALMSPID="$3"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/${ORG,,}.hl7-fhir.com/peers/peer0.${ORG,,}.hl7-fhir.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/${ORG,,}.hl7-fhir.com/users/Admin@${ORG,,}.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:${PORT}

#get last config channelhl7fhir
peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com -c channelhl7fhir --tls --cafile "$ORDERER_CA"

#extract in folder channel-artifacts the configuration
cd channel-artifacts
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
jq '.data.data[0].payload.data.config' config_block.json > config.json
cp config.json config_copy.json

jq ".channel_group.groups.Application.groups.${CORE_PEER_LOCALMSPID}.values += {\"AnchorPeers\":{\"mod_policy\": \"Admins\",\"value\":{\"anchor_peers\": [{\"host\": \"peer0.${ORG,,}.hl7-fhir.com\",\"port\": ${PORT}}]},\"version\": \"0\"}}" config_copy.json > modified_config.json

configtxlator proto_encode --input config.json --type common.Config --output config.pb
configtxlator proto_encode --input modified_config.json --type common.Config --output modified_config.pb
configtxlator compute_update --channel_id channelhl7fhir --original config.pb --updated modified_config.pb --output config_update.pb
configtxlator proto_decode --input config_update.pb --type common.ConfigUpdate --output config_update.json
echo '{"payload":{"header":{"channel_header":{"channel_id":"channelhl7fhir", "type":2}},"data":{"config_update":'$(cat config_update.json)'}}}' | jq . > config_update_in_envelope.json
configtxlator proto_encode --input config_update_in_envelope.json --type common.Envelope --output config_update_in_envelope.pb
cd ..

#update new configuration of the channel for node org1 is anchor peer
peer channel update -f channel-artifacts/config_update_in_envelope.pb -c channelhl7fhir -o localhost:7050  --ordererTLSHostnameOverride orderer.hl7-fhir.com --tls --cafile "${PWD}/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem"

echo "*******************************************************"
echo "*****************End anchor peer ${ORG}*****************"
echo "*******************************************************"