# ---------- Build stage: compile the app into a runnable jar ----------
FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Download dependencies first so later builds can reuse this layer
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Run stage: small image with only the Java runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Tuned for small free-tier containers (about 512 MB of memory)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=180m"
ENV SHOW_SQL=false

# Render provides the PORT variable; the app reads it (defaults to 8081 locally)
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
