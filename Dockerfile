FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=America/Sao_Paulo

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/api/auth/csrf || exit 1

ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "/app/app.jar"]
