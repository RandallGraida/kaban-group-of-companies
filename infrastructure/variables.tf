variable "aws_region" {
  description = "The AWS region to deploy to"
  type        = string
  default     = "ap-southeast-2"
}

variable "project_name" {
  description = "Project name prefix"
  type        = string
  default     = "kaban"
}

variable "db_password" {
  description = "Master password for the RDS database"
  type        = string
  sensitive   = true
}
