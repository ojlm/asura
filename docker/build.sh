#!/usr/bin/env bash
docker build --file ./Dockerfile -t indigo-api ../
docker tag indigo-api asurapro/indigo-api
docker push asurapro/indigo-api
