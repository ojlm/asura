FROM openjdk:8
COPY ./target/universal/stage /opt/asura
EXPOSE 9000
CMD /opt/asura/bin/asura \
  -J-Xms128M -J-Xmx1048m \
  -Dconfig.resource=docker.conf \
  -Dpidfile.path=/dev/null
