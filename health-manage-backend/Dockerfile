FROM maven:3.8.6-openjdk-8 AS builder
WORKDIR /app

# Copy source
COPY pom.xml .
COPY src ./src

# Build jar
RUN mvn -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
