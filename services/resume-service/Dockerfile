FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/resume-service-0.1.0.jar app.jar
COPY --from=build /app/target/classes/com/tailorcv/resume/HealthProbe.class /app/com/tailorcv/resume/HealthProbe.class
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
