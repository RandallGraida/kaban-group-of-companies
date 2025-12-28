FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the entire project (respecting .dockerignore)
COPY . .

# Grant execution rights
RUN chmod +x mvnw

# Build only the transaction-service module
RUN ./mvnw -pl transaction-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/transaction-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
