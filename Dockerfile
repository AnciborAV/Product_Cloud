FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN sh ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /build/target/Spring_DEMO-0.0.1-SNAPSHOT.jar /app/app.jar
USER app:app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -Djava.awt.headless=true"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
