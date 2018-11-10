#!/usr/bin/env bash
docker build --file ./Dockerfile -t asurapro/indigo-api ../
docker push asurapro/indigo-api
