FROM openjdk:18-slim

COPY target/launcher-0.0.1-SNAPSHOT.jar /launcher.jar

CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar /launcher.jar
