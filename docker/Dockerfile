FROM openjdk:8
ADD ./target/universal/stage /opt/asura
EXPOSE 9000
ENTRYPOINT /opt/asura/bin/asura \
  -J-Xms128M -J-Xmx1048m \
  -Dconfig.resource=docker.conf
