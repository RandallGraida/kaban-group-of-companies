# Kaban Core Banking System CI/CD & Infrastructure Guide

**GitLab CI | Docker | Terraform | AWS ECS | SonarQube**

This guide outlines the production-grade Continuous Integration and Continuous Deployment (CI/CD) pipeline for the Kaban Core Banking System. It leverages Infrastructure as Code (IaC) to manage AWS resources and ensures strict quality gates before deployment.

---

## 1. Pipeline Architecture

The pipeline is designed with the following stages. Failures in any stage (except manual gates) stop the pipeline immediately.

| Stage | Description | Tools |
| :--- | :--- | :--- |
| **Test** | Runs unit tests and verifies code compilation. | Maven, JUnit |
| **Security** | Static Application Security Testing (SAST) and Code Quality checks. | SonarQube |
| **Build** | Builds the Docker image and pushes it to AWS ECR. | Docker, AWS CLI |
| **Infra-Plan** | Generates a Terraform plan to preview infrastructure changes. | Terraform |
| **Infra-Apply** | Applies the Terraform plan to update AWS infrastructure. | Terraform |
| **Deploy** | Updates the ECS Service to use the new Docker image. | AWS CLI / ECS |

---

## 2. Docker Strategy

We use a **Multi-Stage Build** to keep the production image footprint small (removing Maven, source code, and build tools) and leverage layer caching.

### `Dockerfile`
Place this in the root of each microservice directory (e.g., `auth-service/`).

```dockerfile
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (Cached if pom.xml hasn't changed)
COPY ../pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY ../src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Image (Distroless or Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy only the built artifact
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## 3. Infrastructure as Code (Terraform)

We use Terraform to manage AWS resources (ECR, ECS, RDS, VPC).
**State Management:** We use GitLab's managed Terraform State as the backend.

### Directory Structure
```text
/infrastructure
├── main.tf         # Provider & Backend config
├── variables.tf    # Input variables
├── ecs.tf          # ECS Cluster & Service definitions
└── rds.tf          # Database definitions
```

### `infrastructure/main.tf`
This configuration allows GitLab to store the state file securely.

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # GitLab Managed Terraform State
  backend "http" {
  }
}

provider "aws" {
  region = var.aws_region
}
```

---

## 4. GitLab CI Configuration

Create `.gitlab-ci.yml` in the project root. This pipeline handles both Staging (auto-deploy) and Production (manual approval).

### `.gitlab-ci.yml`

```yaml
image: docker:latest

services:
  - docker:dind

variables:
  # AWS Configuration
  AWS_DEFAULT_REGION: "ap-southeast-1"
  DOCKER_DRIVER: overlay2
  
  # Terraform State Address
  TF_ADDRESS: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${CI_COMMIT_REF_SLUG}
  
  # Image Tagging
  ECR_REPO_URL: "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/kaban"
  IMAGE_TAG: "$CI_COMMIT_SHA"

stages:
  - test
  - security
  - build
  - infra-plan
  - infra-apply
  - deploy

# Cache Maven dependencies
cache:
  paths:
    - .m2/repository

# ==========================================
# 1. TEST STAGE
# ==========================================
unit-test:
  image: maven:3.9-eclipse-temurin-17-alpine
  stage: test
  script:
    - mvn test
  artifacts:
    reports:
      junit: "**/target/surefire-reports/*.xml"

# ==========================================
# 2. SECURITY STAGE (SonarQube)
# ==========================================
sonar-check:
  image: maven:3.9-eclipse-temurin-17-alpine
  stage: security
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  script:
    - mvn verify sonar:sonar -Dsonar.projectKey=kaban-core-banking -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_TOKEN
  allow_failure: false
  only:
    - dev
    - main

# ==========================================
# 3. BUILD STAGE (Docker)
# ==========================================
docker-build:
  stage: build
  before_script:
    - apk add --no-cache python3 py3-pip
    - pip3 install awscli
    - aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REPO_URL
  script:
    # Example for one service, repeat for others
    - cd auth-service
    - docker build -t $ECR_REPO_URL/auth-service:$IMAGE_TAG .
    - docker push $ECR_REPO_URL/auth-service:$IMAGE_TAG

# ==========================================
# 4. INFRASTRUCTURE (Terraform)
# ==========================================
.terraform-setup: &terraform_setup
  image: hashicorp/terraform:light
  before_script:
    - cd infrastructure
    - terraform init -backend-config="address=${TF_ADDRESS}" -backend-config="lock_address=${TF_ADDRESS}/lock" -backend-config="unlock_address=${TF_ADDRESS}/lock" -backend-config="username=gitlab-ci-token" -backend-config="password=${CI_JOB_TOKEN}" -backend-config="lock_method=POST" -backend-config="unlock_method=DELETE" -backend-config="retry_wait_min=5"

plan:
  <<: *terraform_setup
  stage: infra-plan
  script:
    - terraform plan -out=tfplan -var="image_tag=$IMAGE_TAG"
  artifacts:
    paths:
      - infrastructure/tfplan

apply:
  <<: *terraform_setup
  stage: infra-apply
  script:
    - terraform apply -auto-approve tfplan
  dependencies:
    - plan
  only:
    - dev  # Auto-apply on Dev
  when: manual # Manual approval for Prod (if on main)

# ==========================================
# 5. DEPLOY STAGE (ECS Update)
# ==========================================
deploy-ecs:
  stage: deploy
  image: registry.gitlab.com/gitlab-org/cloud-deploy/aws-base:latest
  script:
    # Example for one service, repeat for others
    - aws ecs update-service --cluster kaban-cluster --service auth-service --force-new-deployment
  only:
    - dev
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
| `SONAR_HOST_URL` | Variable | No | No | URL of your SonarQube server. |
| `SONAR_TOKEN` | Variable | Yes | Yes | Token generated in SonarQube. |

### IAM Permissions
The AWS User used in CI/CD should adhere to **Least Privilege**.
*   **ECR:** `GetAuthorizationToken`, `BatchCheckLayerAvailability`, `PutImage`.
*   **ECS:** `UpdateService`, `DescribeServices`.
*   **Terraform:** Needs broader permissions (VPC, RDS, IAM) but should be restricted to the specific region and resource tags.

### Environment Isolation
*   **Staging:** Deployed from `dev` branch. Uses `terraform.workspace` or separate state files to ensure isolation.
*   **Production:** Deployed from `main` branch. Requires **Manual Approval** in the pipeline before `infra-apply`.
