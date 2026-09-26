# Etap 1: Ekstrakcja warstw z artefaktu JAR
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Etap 2: Złożenie finalnego obrazu uruchomieniowego
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

# Kopiowanie warstw od najrzadziej do najczęściej zmienianych
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]