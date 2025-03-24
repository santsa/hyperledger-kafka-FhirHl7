#!/bin/bash

echo "*******************************************************"
echo "*******************launch explorer*********************"
echo "*******************************************************"
docker-compose -f network/docker-compose-explorer.yaml down -v
sleep 10
docker-compose -f network/docker-compose-explorer.yaml up -d