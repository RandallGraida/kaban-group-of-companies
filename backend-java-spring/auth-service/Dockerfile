FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the entire project (respecting .dockerignore)
COPY . .

# Grant execution rights
RUN chmod +x mvnw

# Build only the auth-service module
# We also include -am (also make dependents) just in case, though usually not needed if parent is just a pom.
RUN ./mvnw -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
