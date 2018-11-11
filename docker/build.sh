#!/usr/bin/env bash
cd ..
sbt clean dist
cd target/universal
unzip asura-app-*.zip
rm asura-app-*.zip
mv asura-app-* asura-app
cd ../../docker
docker build --file ./Dockerfile -t asurapro/indigo-api ../
docker push asurapro/indigo-api
