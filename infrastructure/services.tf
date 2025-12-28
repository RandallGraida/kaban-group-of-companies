# --- Task Definitions & Services ---

# 1. Auth Service
resource "aws_ecs_task_definition" "auth_service" {
  family                   = "auth-service"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = 256
  memory                   = 200 # Soft Limit (allows burst if available)

  container_definitions = jsonencode([
    {
      name      = "auth-service"
      image     = aws_ecr_repository.auth_service.repository_url
      cpu       = 256
      memory    = 200
      essential = true
      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8081" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/kaban_db" },
        { name = "SPRING_DATASOURCE_USERNAME", value = "postgres" },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
        { name = "APP_JWT_SECRET", value = var.jwt_secret }
      ],
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/kaban-services"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "auth"
          "awslogs-create-group"  = "true"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "auth_service" {
  name            = "auth-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.auth_service.arn
  desired_count   = 1
}

# 2. Account Service
resource "aws_ecs_task_definition" "account_service" {
  family                   = "account-service"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = 256
  memory                   = 200

  container_definitions = jsonencode([
    {
      name      = "account-service"
      image     = aws_ecr_repository.account_service.repository_url
      cpu       = 256
      memory    = 200
      essential = true
      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
        }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8082" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/kaban_db" },
        { name = "SPRING_DATASOURCE_USERNAME", value = "postgres" },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" }
      ],
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/kaban-services"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "account"
          "awslogs-create-group"  = "true"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "account_service" {
  name            = "account-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.account_service.arn
  desired_count   = 1
}

# 3. Transaction Service
resource "aws_ecs_task_definition" "transaction_service" {
  family                   = "transaction-service"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = 256
  memory                   = 200

  container_definitions = jsonencode([
    {
      name      = "transaction-service"
      image     = aws_ecr_repository.transaction_service.repository_url
      cpu       = 256
      memory    = 200
      essential = true
      portMappings = [
        {
          containerPort = 8083
          hostPort      = 8083
        }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8083" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/kaban_db" },
        { name = "SPRING_DATASOURCE_USERNAME", value = "postgres" },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" }
      ],
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/kaban-services"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "transaction"
          "awslogs-create-group"  = "true"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "transaction_service" {
  name            = "transaction-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.transaction_service.arn
  desired_count   = 1
}

# 4. Notification Service
resource "aws_ecs_task_definition" "notification_service" {
  family                   = "notification-service"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = 256
  memory                   = 200

  container_definitions = jsonencode([
    {
      name      = "notification-service"
      image     = aws_ecr_repository.notification_service.repository_url
      cpu       = 256
      memory    = 200
      essential = true
      portMappings = [
        {
          containerPort = 8084
          hostPort      = 8084
        }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8084" },
        { name = "AUTH_SERVICE_URL", value = "http://localhost:8081" } # Since they share the host network via bridge
      ],
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/kaban-services"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "notification"
          "awslogs-create-group"  = "true"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "notification_service" {
  name            = "notification-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.notification_service.arn
  desired_count   = 1
}
