# Stage 1 - Build: compile Java code into JAR
FROM maven:3.9-eclipse-temurin-21-jammy AS build
WORKDIR /app
# Copy pom.xml first - Docker caches dependencies (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -q
# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 - Run: just the JAR, no Maven needed
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
