# Kaban Banking Backend Developer Guide

This guide provides instructions for setting up, developing, and deploying the Kaban Banking microservices.

## 🏗 Architecture Overview

The system consists of several Spring Boot microservices:
- **Auth Service (8081):** User registration, login, and JWT management.
- **Account Service (8082):** Management of user bank accounts and profiles.
- **Transaction Service (8083):** Core banking operations (deposits, transfers).
- **Notification Service (8084):** Email delivery via SMTP/MailHog.

## 🚀 Local Development (Native)

### Prerequisites
- Java 17
- Maven
- PostgreSQL 15+

### Setup
1. Create a PostgreSQL database named `kaban_db`.
2. Configure `application.properties` in each service's `src/main/resources` folder (use the `.example` files as templates).
3. Build the project:
   ```bash
   ./mvnw clean install
   ```
4. Run a service:
   ```bash
   ./mvnw spring-boot:run -pl auth-service
   ```

## 🐳 Local Development with Docker

You can spin up the entire Kaban infrastructure and services using Docker Compose.

### 1. Configuration
The `docker-compose.yaml` uses environment variables with safe defaults. You can override them by creating a `.env` file in the root:
```env
DB_USERNAME=randallgraida
DB_PASSWORD=your_secure_password
DB_NAME=kaban_db
```

### 2. Start Infrastructure
To start only the supporting services (Postgres and MailHog):
```bash
docker compose up -d postgres mailhog
```

### 3. Run the Entire System
To build and start all microservices together:
```bash
docker compose up --build
```

### 4. Useful Docker Commands
- **Stop all services:** `docker compose down`
- **View logs for a service:** `docker compose logs -f auth-service`
- **Restart a specific service:** `docker compose restart transaction-service`

## 🛠 Database Management
- **Local Host:** `localhost`
- **Port:** `5432`
- **Database Name:** `kaban_db`
- **Shared Schema:** Currently, all services share `kaban_db` for simplified local development.

## 📧 Email Testing
All outbound emails are captured by **MailHog** during local development.
- **Web UI:** [http://localhost:8025](http://localhost:8025)
- **SMTP Port:** `1025`