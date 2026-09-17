FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY integration-events/pom.xml integration-events/pom.xml
COPY account-domain/pom.xml account-domain/pom.xml
COPY account-application/pom.xml account-application/pom.xml
COPY account-infrastructure/pom.xml account-infrastructure/pom.xml
COPY account-bootstrap/pom.xml account-bootstrap/pom.xml
COPY notification-application/pom.xml notification-application/pom.xml
COPY notification-infrastructure/pom.xml notification-infrastructure/pom.xml
COPY notification-bootstrap/pom.xml notification-bootstrap/pom.xml

COPY integration-events/src integration-events/src
COPY notification-application/src notification-application/src
COPY notification-infrastructure/src notification-infrastructure/src
COPY notification-bootstrap/src notification-bootstrap/src
RUN mvn -B -pl notification-bootstrap -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/notification-bootstrap/target/notification-bootstrap-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
