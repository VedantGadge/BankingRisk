# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Dependency resolution layer (caches dependencies if pom.xml doesn't change)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Source code layer
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage (lightweight alpine image)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user and group for security hardening
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from builder stage
COPY --from=builder /app/target/BankingProject-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

