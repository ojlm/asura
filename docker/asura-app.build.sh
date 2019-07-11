#!/usr/bin/env bash
cd ..
sbt clean asura-app/dist
cd asura-app/target/universal
unzip asura-app-*.zip
rm asura-app-*.zip
mv asura-app-* asura-app
cd ../../../docker
_tag=$1
if [ -z "${_tag}" ]; then
    _tag=latest
fi
docker build --file ./asura-app.dockerfile -t "asurapro/asura:${_tag}" ../
docker push asurapro/asura
