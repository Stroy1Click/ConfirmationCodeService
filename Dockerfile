FROM eclipse-temurin:21-jre-alpine
LABEL authors="egorm"

WORKDIR /app
COPY maven/confirmation-code-service-0.0.1-SNAPSHOT.jar /app/confirmation.jar
EXPOSE 6060
ENTRYPOINT ["java", "-jar", "confirmation.jar"]