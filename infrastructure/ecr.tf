resource "aws_ecr_repository" "auth_service" {
  name                 = "kaban/auth-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "aws_ecr_repository" "account_service" {
  name                 = "kaban/account-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "aws_ecr_repository" "transaction_service" {
  name                 = "kaban/transaction-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "aws_ecr_repository" "notification_service" {
  name                 = "kaban/notification-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}
