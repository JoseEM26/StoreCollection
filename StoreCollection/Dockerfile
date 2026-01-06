# ----------- BUILD STAGE -------------
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias (cache build)
COPY pom.xml .
RUN mvn -B dependency:resolve dependency:resolve-plugins

# Copiar el proyecto completo
COPY . .

# Construir el JAR
RUN mvn -B clean package -DskipTests


# ----------- RUN STAGE --------------
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copiar solo el JAR construido
COPY --from=build /app/target/*.jar app.jar

# Variables de entorno para Railway
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
