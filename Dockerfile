FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/documind-ai-0.0.1-SNAPSHOT.jar app.jar
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseContainerSupport", "-jar", "app.jar"]
