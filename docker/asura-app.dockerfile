FROM openjdk:8
COPY ./asura-app/target/universal/asura-app /opt/asura-app
EXPOSE 9000
CMD /opt/asura-app/bin/asura-app \
  -J-Xms128M -J-Xmx1048m \
  -Dconfig.resource=docker.conf \
  -Dpidfile.path=/dev/null
