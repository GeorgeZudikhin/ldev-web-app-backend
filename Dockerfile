# stage 1
FROM maven:3.8.1-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# stage 2
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/web-app-backend-0.0.1-SNAPSHOT.jar web-app-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "web-app-backend.jar"]