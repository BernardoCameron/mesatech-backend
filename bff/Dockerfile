FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Docker en Windows a veces resuelve hosts externos (login.microsoftonline.com)
# solo a una direccion IPv6 sin salida real, dando "Network unreachable" al
# validar el JWT contra el JWKS de Entra ID. Forzamos IPv4 para evitarlo.
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
