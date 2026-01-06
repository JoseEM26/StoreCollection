# ----------- BUILD STAGE -------------
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:resolve dependency:resolve-plugins

COPY . .
RUN mvn -B clean package -DskipTests

# ----------- RUN STAGE --------------
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
