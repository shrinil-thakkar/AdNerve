variable "aws_region" {
  type        = string
  default     = "ap-south-1"
  description = "AWS region"
}

variable "app_name" {
  type        = string
  default     = "cms"
  description = "Prefix for all resource names"
}

variable "ecr_image_uri" {
  type        = string
  default     = "872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest"
  description = "Full ECR image URI"
}

variable "s3_media_bucket_name" {
  type        = string
  description = "Globally unique S3 bucket name for media files"
}

variable "domain_name" {
  type        = string
  description = "Full subdomain for the API e.g. api.yourdomain.com"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "RDS PostgreSQL password"
}

variable "jwt_secret" {
  type        = string
  sensitive   = true
  description = "JWT signing secret (min 32 chars)"
}

variable "db_username" {
  type        = string
  default     = "cms_user"
  description = "RDS master username"
}

variable "db_name" {
  type        = string
  default     = "cms_db"
  description = "RDS database name"
}

variable "container_port" {
  type        = number
  default     = 8080
  description = "Port the Spring Boot app listens on"
}

variable "task_cpu" {
  type        = number
  default     = 512
  description = "Fargate task CPU units (512 = 0.5 vCPU)"
}

variable "task_memory" {
  type        = number
  default     = 1024
  description = "Fargate task memory in MB"
}
