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