#!/bin/bash
#sudo -u sgorrita ./scripts/00-start.sh addorg-chaincode
echo "*******************************************************"
echo "******Init 06-start-chaincode-hl7-fhir-java-org4*******"
echo "*******************************************************"

export PATH_HOME=${PWD}
export PATH=$PATH_HOME/bin:$PATH_HOME:$PATH
export FABRIC_CFG_PATH=$PATH_HOME/config
export VERSION="1.0"
export SEQUENCE="2"
export CORE_PEER_TLS_ENABLED=true


echo "*******************************************************"
echo "*************install chaincode peer 4****************"
echo "*******************************************************"
#change peer org4 and install
export CORE_PEER_LOCALMSPID="Org4MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=$PATH_HOME/organizations/peerOrganizations/org4.hl7-fhir.com/peers/peer0.org4.hl7-fhir.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org4.hl7-fhir.com/users/Admin@org4.hl7-fhir.com/msp
export CORE_PEER_ADDRESS=localhost:13051
peer lifecycle chaincode install hl7-fhir-java.tar.gz

echo "*******************************************************"
echo "***************queryinstalled chaincode****************"
echo "*******************************************************"
peer lifecycle chaincode queryinstalled
#export CC_PACKAGE_ID=hl7-fhir-java_1.0:xxxxxxxxxx.....
export CC_CHAIN_CODE_LABEL="hl7-fhir-java_$VERSION"
export CC_PACKAGE_ID=$(peer lifecycle chaincode queryinstalled | grep "$CC_CHAIN_CODE_LABEL" | awk -F 'Package ID: ' '{print $2}' | awk -F ', Label:' '{print $1}')

echo "*******************************************************"
echo "***********approveformyorg chaincode peer 1************"
echo "*******************************************************"
sleep 10
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org1.hl7-fhir.com/users/Admin@org1.hl7-fhir.com/msp
export CORE_PEER_TLS_ROOTCERT_FILE=$PATH_HOME/organizations/peerOrganizations/org1.hl7-fhir.com/peers/peer0.org1.hl7-fhir.com/tls/ca.crt
export CORE_PEER_ADDRESS=localhost:7051
peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com --channelID channelhl7fhir --signature-policy "OR('Org1MSP.member','Org2MSP.member','Org4MSP.member')" --name hl7-fhir-java --version $VERSION --package-id $CC_PACKAGE_ID --sequence $SEQUENCE --tls --cafile $PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

echo "*******************************************************"
echo "***********approveformyorg chaincode peer 2************"
echo "*******************************************************"
sleep 10
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org2.hl7-fhir.com/users/Admin@org2.hl7-fhir.com/msp
export CORE_PEER_TLS_ROOTCERT_FILE=$PATH_HOME/organizations/peerOrganizations/org2.hl7-fhir.com/peers/peer0.org2.hl7-fhir.com/tls/ca.crt
export CORE_PEER_ADDRESS=localhost:9051
peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com --channelID channelhl7fhir --signature-policy "OR('Org1MSP.member','Org2MSP.member','Org4MSP.member')" --name hl7-fhir-java --version $VERSION --package-id $CC_PACKAGE_ID --sequence $SEQUENCE --tls --cafile $PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

echo "*******************************************************"
echo "***********approveformyorg chaincode peer 4************"
echo "*******************************************************"
sleep 10
export CORE_PEER_LOCALMSPID="Org4MSP"
export CORE_PEER_MSPCONFIGPATH=$PATH_HOME/organizations/peerOrganizations/org4.hl7-fhir.com/users/Admin@org4.hl7-fhir.com/msp
export CORE_PEER_TLS_ROOTCERT_FILE=$PATH_HOME/organizations/peerOrganizations/org4.hl7-fhir.com/peers/peer0.org4.hl7-fhir.com/tls/ca.crt
export CORE_PEER_ADDRESS=localhost:13051
peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.hl7-fhir.com --channelID channelhl7fhir --signature-policy "OR('Org1MSP.member','Org2MSP.member','Org4MSP.member')" --name hl7-fhir-java --version $VERSION --package-id $CC_PACKAGE_ID --sequence $SEQUENCE --tls --cafile $PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem

echo "*******************************************************"
echo "(checkcommitreadiness - might need adjustment to check for Org4)"
echo "*******************************************************"
sleep 10
peer lifecycle chaincode checkcommitreadiness --channelID channelhl7fhir --name hl7-fhir-java --version $VERSION --sequence $SEQUENCE --tls --cafile $PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem --output json

echo "*******************************************************"
echo "***************commit chaincode***********************"
echo "*******************************************************"
sleep 10
peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride  orderer.hl7-fhir.com --signature-policy "OR('Org1MSP.member','Org2MSP.member','Org4MSP.member')" --channelID channelhl7fhir --name hl7-fhir-java --version $VERSION --sequence $SEQUENCE --tls --cafile $PATH_HOME/organizations/ordererOrganizations/hl7-fhir.com/orderers/orderer.hl7-fhir.com/msp/tlscacerts/tlsca.hl7-fhir.com-cert.pem --peerAddresses localhost:7051 --tlsRootCertFiles $PATH_HOME/organizations/peerOrganizations/org1.hl7-fhir.com/peers/peer0.org1.hl7-fhir.com/tls/ca.crt --peerAddresses localhost:9051 --tlsRootCertFiles $PATH_HOME/organizations/peerOrganizations/org2.hl7-fhir.com/peers/peer0.org2.hl7-fhir.com/tls/ca.crt --peerAddresses localhost:13051 --tlsRootCertFiles $PATH_HOME/organizations/peerOrganizations/org4.hl7-fhir.com/peers/peer0.org4.hl7-fhir.com/tls/ca.crt 

echo "*******************************************************"
echo "**************query chaincode GetAllAssets*************"
echo "*******************************************************"
sleep 5
peer chaincode query -C channelhl7fhir -n hl7-fhir-java -c '{"Args":["GetAllAssets"]}'

echo "*******************************************************"
echo "***********query chaincode ReadAsset asset1************"
echo "*******************************************************"
peer chaincode query -C channelhl7fhir -n hl7-fhir-java -c '{"Args":["ReadAsset","asset1"]}'

echo "*******************************************************"
echo "*****End 06-start-chaincode-hl7-fhir-java-org4*********"
echo "*******************************************************"