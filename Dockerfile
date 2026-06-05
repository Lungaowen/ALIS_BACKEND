# ─────────────────────────────────────────────
# Stage 1: Build — full Maven multi-module build
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root POM + all module POMs first (layer-cache Maven deps)
COPY pom.xml .
COPY alis-core/pom.xml     alis-core/pom.xml
COPY alis-user/pom.xml     alis-user/pom.xml
COPY alis-auth/pom.xml     alis-auth/pom.xml
COPY alis-ai/pom.xml       alis-ai/pom.xml
COPY alis-legal/pom.xml    alis-legal/pom.xml
COPY alis-api/pom.xml      alis-api/pom.xml

RUN mvn dependency:go-offline -B

# Copy all source
COPY alis-core/src     alis-core/src
COPY alis-user/src     alis-user/src
COPY alis-auth/src     alis-auth/src
COPY alis-ai/src       alis-ai/src
COPY alis-legal/src    alis-legal/src
COPY alis-api/src      alis-api/src

# Build — skip tests in CI; they run separately
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Runtime — slim JRE image
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S alis && adduser -S alis -G alis

# Copy the executable JAR from the api module
COPY --from=build /app/alis-api/target/alis-api-0.0.1-SNAPSHOT.jar app.jar

# Firebase service account (mounted at runtime — do NOT bake into image)
RUN mkdir -p /app/config

USER alis

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
