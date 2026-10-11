FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 ccems && mkdir -p /data/uploads && chown ccems /data/uploads
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER ccems
ENV UPLOAD_DIR=/data/uploads SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 CMD curl -fs http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
