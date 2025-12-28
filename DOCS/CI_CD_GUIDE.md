# Kaban Core Banking System CI/CD & Infrastructure Guide

**GitLab CI | Docker | Terraform | AWS ECS | AWS ECR**

This guide outlines the production-grade Continuous Integration and Continuous Deployment (CI/CD) pipeline for the Kaban Core Banking System. It leverages Infrastructure as Code (IaC) to manage AWS resources and ensures correct build contexts for the monorepo structure.

---

## 1. Pipeline Architecture

The pipeline is designed with the following stages.

| Stage | Description | Tools |
| :--- | :--- | :--- |
| **Test** | Runs unit tests and verifies code compilation. | Maven, JUnit |
| **Security** | Static Application Security Testing (SAST) and Code Quality checks. | SonarQube / SonarCloud |
| **Build** | Builds the Docker image from the Monorepo root and pushes it to AWS ECR. | Docker, AWS CLI |
| **Infra-Plan** | Generates a Terraform plan to preview infrastructure changes. | Terraform |
| **Infra-Apply** | Applies the Terraform plan to update AWS infrastructure. | Terraform |
| **Deploy** | Updates the ECS Service to force a new deployment. | AWS CLI / ECS |

---

## 2. Docker Strategy (Monorepo)

Since this is a multi-module Maven project, we must run the docker build from the **root** directory (`backend-java-spring/`) so that the Dockerfile can access the parent `pom.xml`.

### `Dockerfile`
Each service has its own Dockerfile (e.g., `backend-java-spring/auth-service/Dockerfile`), but it is invoked from the parent folder.

```dockerfile
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the entire backend project (Root context)
COPY . .

# Grant execution rights
RUN chmod +x mvnw

# Build ONLY the specific module (e.g., auth-service)
# -pl = project list, -am = also make dependents
RUN ./mvnw -pl auth-service -am clean package -DskipTests

# Stage 2: Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build Command
You **cannot** run `docker build .` inside the service folder. You must run:
```bash
cd backend-java-spring
docker build -f auth-service/Dockerfile -t <ECR_URL>/auth-service:latest .
```

---

## 3. Infrastructure as Code (Terraform)

We use Terraform to manage AWS resources.
**State Management:** Can be local (for solo dev) or GitLab Managed (for teams).

### Directory Structure
```text
/infrastructure
├── providers.tf    # AWS Provider & Region
├── variables.tf    # Secrets (DB Password, JWT)
├── vpc.tf          # Network
├── rds.tf          # Database
├── ecr.tf          # Container Registry
├── ecs.tf          # Cluster & Auto Scaling
└── services.tf     # Task Definitions
```

---

## 4. GitLab CI Configuration

Create `.gitlab-ci.yml` in the project root.

### `.gitlab-ci.yml`

```yaml
image: docker:latest

services:
  - docker:dind

variables:
  # AWS Configuration
  AWS_DEFAULT_REGION: "ap-southeast-2" # Sydney
  DOCKER_DRIVER: overlay2
  
  # Image Tagging
  ECR_REGISTRY: "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
  
stages:
  - test
  - security
  - build
  - deploy

# ==========================================
# 1. TEST STAGE
# ==========================================
unit-test:
  image: maven:3.9-eclipse-temurin-17-alpine
  stage: test
  script:
    - cd backend-java-spring
    - mvn test

# ==========================================
# 2. SECURITY STAGE (SonarQube)
# ==========================================
# Note: Requires external SonarQube server or SonarCloud
sonar-check:
  image: maven:3.9-eclipse-temurin-17-alpine
  stage: security
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
    GIT_DEPTH: "0"
  cache:
    key: "${CI_JOB_NAME}"
    paths:
      - .sonar/cache
  script:
    - cd backend-java-spring
    - mvn verify sonar:sonar -Dsonar.projectKey=kaban-core-banking -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_TOKEN
  allow_failure: true
  only:
    - main

# ==========================================
# 3. BUILD STAGE (Docker)
# ==========================================
docker-build:
  stage: build
  before_script:
    - apk add --no-cache python3 py3-pip aws-cli
    - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
  script:
    - cd backend-java-spring
    
    # Auth Service
    - docker build -f auth-service/Dockerfile -t $ECR_REGISTRY/kaban/auth-service:$CI_COMMIT_SHA .
    - docker push $ECR_REGISTRY/kaban/auth-service:$CI_COMMIT_SHA
    
    # Account Service
    - docker build -f account-service/Dockerfile -t $ECR_REGISTRY/kaban/account-service:$CI_COMMIT_SHA .
    - docker push $ECR_REGISTRY/kaban/account-service:$CI_COMMIT_SHA

    # Transaction Service
    - docker build -f transaction-service/Dockerfile -t $ECR_REGISTRY/kaban/transaction-service:$CI_COMMIT_SHA .
    - docker push $ECR_REGISTRY/kaban/transaction-service:$CI_COMMIT_SHA
    
    # Notification Service
    - docker build -f notification-service/Dockerfile -t $ECR_REGISTRY/kaban/notification-service:$CI_COMMIT_SHA .
    - docker push $ECR_REGISTRY/kaban/notification-service:$CI_COMMIT_SHA

# ==========================================
# 3. DEPLOY STAGE (ECS Update)
# ==========================================
deploy-ecs:
  stage: deploy
  image: registry.gitlab.com/gitlab-org/cloud-deploy/aws-base:latest
  script:
    # Force ECS to pull the new image (tagged with Commit SHA)
    - aws ecs update-service --cluster kaban-cluster --service auth-service --force-new-deployment
    - aws ecs update-service --cluster kaban-cluster --service account-service --force-new-deployment
    - aws ecs update-service --cluster kaban-cluster --service transaction-service --force-new-deployment
    - aws ecs update-service --cluster kaban-cluster --service notification-service --force-new-deployment
  only:
    - main
```

---

## 5. Security Best Practices

### GitLab CI Variables
Never hardcode credentials. Go to **Settings > CI/CD > Variables** and add:

| Variable | Type | Protected | Masked | Description |
| :--- | :--- | :--- | :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Variable | Yes | Yes | AWS IAM User Key (CI/CD User). |
| `AWS_SECRET_ACCESS_KEY` | Variable | Yes | Yes | AWS IAM Secret. |
| `AWS_ACCOUNT_ID` | Variable | Yes | Yes | Your 12-digit AWS Account ID. |

### Manual Deployments (Local)
If CI/CD is down, you can manually deploy from your terminal:

```bash
# 1. Login
aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.ap-southeast-2.amazonaws.com

# 2. Build (From Root)
cd backend-java-spring
docker build -f auth-service/Dockerfile -t <ECR_URL>/kaban/auth-service:latest .

# 3. Push
docker push <ECR_URL>/kaban/auth-service:latest

# 4. Restart Service
aws ecs update-service --cluster kaban-cluster --service auth-service --force-new-deployment
```