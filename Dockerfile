FROM openjdk:21
LABEL authors="egorm"

WORKDIR /app
ADD maven/confirmation-code-service-0.0.1-SNAPSHOT.jar /app/confirmation.jar
EXPOSE 6060
ENTRYPOINT ["java", "-jar", "confirmation.jar"]