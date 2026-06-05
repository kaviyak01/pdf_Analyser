



# ── STAGE 1: BUILD ──────────────────────────────────────────
# Use Maven + Java 17 image to build the JAR
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory inside container
WORKDIR /app

# Copy pom.xml first — Docker caches dependencies separately
# If pom.xml hasn't changed, this layer is reused (faster builds)
COPY pom.xml .

# Download all dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy all source code
COPY src ./src

# Build the JAR, skip tests for faster build
RUN mvn clean package -DskipTests

# ── STAGE 2: RUN ────────────────────────────────────────────
# Use lightweight Java 17 runtime only (no Maven needed to run)
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy only the built JAR from Stage 1
# This keeps the final image small — no source code, no Maven
COPY --from=builder /app/target/*.jar app.jar

# Expose port (documentation only — Render sets actual port via ENV)
EXPOSE 8080

# Run the JAR
# ${PORT} is injected by Render at runtime
ENTRYPOINT ["java", "-jar", "app.jar"]