# Kaban Core Banking System Backend Developer Guide

**Java Spring Boot | Lightweight Microservices | Nginx | AWS Free Tier**

## Table of Contents
- [Architecture Overview (Free Tier Optimized)](#architecture-overview-free-tier-optimized)
- [Service Responsibilities](#service-responsibilities)
- [Event-Driven Flow: User Signup](#event-driven-flow-user-signup)
- [Tech Stack](#tech-stack)
- [Free Tier Deployment Guide](#free-tier-deployment-guide)
- [Nginx Gateway Configuration](#nginx-gateway-configuration)

---

## Architecture Overview (Free Tier Optimized)

This architecture retains the **full business logic** (all 4 microservices) and **asynchronous messaging** (SQS/SNS) while being optimized to run on the AWS Free Tier. CloudFront is integrated for improved performance and security.

**Key Optimizations:**
1.  **Lightweight Gateway:** Uses **Nginx** instead of Spring Cloud Gateway to save ~250MB RAM.
2.  **No Service Registry:** Uses **Docker's internal DNS** for service discovery, saving ~250MB RAM.
3.  **Swap Space:** Leverages a **Swap File** on the EC2 instance to prevent Out-of-Memory crashes.
4.  **CDN:** **AWS CloudFront** for global content delivery and API caching.

```text
+------------------+      +------------------+      +---------------------+      +--------------------------------+
|      User        |----->|    Route 53      |----->|     CloudFront      |----->|          Nginx Gateway         |
| (via Browser)    |      |  (Paid Service)  |      |  (Free Tier CDN)    |      |      (EC2 t3.micro)            |
+------------------+      +------------------+      +----------+----------+      +------------------+-------------+
                                                                | (Origin)                            | (Routes To)
                                                                +-------------------------------------+
                                                                                                      |
        +------------------------------------+-------------------------+-------------------------+
        | (HTTP)             | (HTTP)                | (HTTP)                  | (Event)         |
        v                    v                       v                         v                 v
+--------------+    +-----------------+    +---------------------+   +------------------+   +---------+
| Auth Service |    | Account Service |    | Transaction Service |-->|  AWS SQS / SNS   |-->|  AWS SES|
| (Spring Boot)|    | (Spring Boot)   |    | (Spring Boot)       |   | (Free Tier Queue)|   |  (Email)|
+--------------+    +-----------------+    +---------------------+   +------------------+   +---------+
        |                    |                       |
        v                    v                       v
+------------------------------------------------------------------+
|                   PostgreSQL RDS (db.t3.micro)                   |
|     (Schemas: auth_service, account_service, transaction_service)|
+------------------------------------------------------------------+
```

## Service Responsibilities

| Service | Port (Internal) | Responsibility |
| :--- | :--- | :--- |
| **AWS CloudFront** | `443` | Global CDN for frontend assets and API caching. Improves latency and reduces load on Nginx. |
| **Nginx Gateway** | `80` | Origin for CloudFront. Handles routing and SSL. |
| **Auth Service** | `8081` | User authentication and registration. Publishes `USER_REGISTERED` event to SNS. |
| **Account Service** | `8082` | Manages user profiles and account details. |
| **Transaction Service**| `8083` | Handles core banking operations. |
| **Notification Service**|`8084`| Subscribes to SNS via an SQS queue. Sends emails using **AWS SES**. |

## Event-Driven Flow: User Signup
1.  A user signs up via **CloudFront**, which forwards the request to the **Nginx Gateway**, then to the **Auth Service**.
2.  The **Auth Service** creates the user in its database.
3.  The **Auth Service** publishes a `USER_REGISTERED` message to an **AWS SNS Topic**.
4.  The **Notification Service**, subscribed via an **SQS Queue**, receives the message.
5.  The **Notification Service** uses **AWS SES** to send a welcome email.

## Tech Stack
*   **CDN:** AWS CloudFront
*   **Gateway:** Nginx
*   **Business Logic:** Java 17, Spring Boot 3
*   **Database:** PostgreSQL (on RDS)
*   **Messaging:** AWS SQS & SNS
*   **Email:** AWS SES
*   **Networking:** Route 53 (Paid), Docker DNS
*   **Cloud:** AWS Free Tier (EC2 `t3.micro`, RDS `db.t3.micro`, CloudFront)

---

## Free Tier Deployment Guide

To run **4 Java Services + Nginx** on a single `t3.micro` (1GB RAM), you **must** follow these steps.

### 1. CRITICAL: Enable Swap Space
This is the key to stability. Run these commands on your EC2 instance **before** deploying.
```bash
# 1. Create a 2GB swap file
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 2. Make it permanent
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 3. Verify
free -h
# You should see "Swap: 2.0Gi"
```

### 2. JVM Optimization
Apply these flags to every Spring Boot `Dockerfile` to limit RAM usage.
```dockerfile
# In every service's Dockerfile
ENV JAVA_OPTS="-Xmx180m -Xms180m -Xss256k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Dspring.main.lazy-initialization=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### 3. Service Discovery (No Eureka)
Services communicate using their Docker container names.
*   **Bad (Eureka):** `@LoadBalanced RestTemplate`
*   **Good (Docker DNS):** `RestClient.create("http://account-service:8082")`

---

## Nginx Gateway Configuration

Create a folder `/nginx-gateway` with these files.

### `nginx.conf`
```nginx
events {
    worker_connections 1024;
}

http {
    server {
        listen 80;
        # Replace with your domain from Route 53
        server_name your-domain.com;

        # Routes
        location /api/auth/ { proxy_pass http://auth-service:8081/; }
        location /api/accounts/ { proxy_pass http://account-service:8082/; }
        location /api/transactions/ { proxy_pass http://transaction-service:8083/; }
    }
}
```

### `Dockerfile` (for Nginx)
```dockerfile
FROM nginx:alpine
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
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