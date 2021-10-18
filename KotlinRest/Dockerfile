FROM maven:3.8.3-openjdk-17-slim as builder

COPY src /src
COPY pom.xml /pom.xml

RUN mvn clean package

CMD find /target/ -name *.jar | xargs java -jar