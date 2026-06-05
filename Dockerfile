# syntax=docker/dockerfile:1
# ─── Build ────────────────────────────────────────────────────────────────────
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    gradle bootJar --no-daemon -x test

# ─── Runtime ──────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/build/libs/service-portal-manager.jar app.jar

# ── MongoDB ────────────────────────────────────────────────────────────────────
ENV MONGODB_URI=mongodb://localhost:27017/service-portal-manager \
    MONGODB_DATABASE=service-portal-manager

# ── Segurança ──────────────────────────────────────────────────────────────────
ENV JWT_SECRET=CHANGE_ME_PLEASE_THIS_IS_A_DEV_ONLY_SECRET_AT_LEAST_64_CHARS_LONG_FOR_HS512 \
    JWT_EXPIRATION=3600 \
    JWT_ISSUER=service-portal-manager \
    MANAGER_ADMIN_USERNAME=admin \
    MANAGER_ADMIN_PASSWORD=admin

# ── Orquestrador (invalidação de cache cross-service) ────────────────────────────
ENV ORCHESTRATOR_URL=http://localhost:8080 \
    ORCHESTRATOR_ADMIN_USERNAME=admin \
    ORCHESTRATOR_ADMIN_PASSWORD=admin

# ── Servidor ───────────────────────────────────────────────────────────────────
ENV SERVER_PORT=8082 \
    SPRING_PROFILES_ACTIVE="" \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
