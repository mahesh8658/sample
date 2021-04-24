FROM nexus.d.lowes.com:8800/digital/irs-image-jdk:8u212-alpine3.9

USER root
WORKDIR /app

RUN adduser --uid 10101 -S bisgcsuser

# The application's jar file
ARG JAR_FILE=target/bisgcsfileprocessor-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} bisgcsfileprocessor-0.0.1-SNAPSHOT.jar
RUN chown -R 10101 /app/

USER 10101

EXPOSE 8080

ENTRYPOINT ["java", "-Xms1024m", "-Xmx2048m" , "-Djava.awt.headless=true", "-Dcom.sun.management.jmxremote.rmi.port=9090", "-Dcom.sun.management.jmxremote=true", "-Dcom.sun.management.jmxremote.port=9090", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.local.only=false", "-Djava.rmi.server.hostname=127.0.0.1", "-jar", "/app/bisgcsfileprocessor-0.0.1-SNAPSHOT.jar"]