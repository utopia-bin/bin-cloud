FROM eclipse-temurin:21.0.12_8-jre-alpine-3.23

ARG JAR_FILE

RUN addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /app \
    && chown spring:spring /app

WORKDIR /app
COPY --chown=spring:spring ${JAR_FILE} /app/app.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
