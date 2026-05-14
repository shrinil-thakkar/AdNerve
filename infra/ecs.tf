resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/cms"
  retention_in_days = 30

  tags = { Name = "/ecs/cms" }
}

resource "aws_ecs_cluster" "main" {
  name = "cms-cluster"

  tags = { Name = "cms-cluster" }
}

resource "aws_ecs_task_definition" "main" {
  family                   = "cms-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "cms-backend"
    image = var.ecr_image_uri

    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "APP_ENV",                          value = "production" },
      { name = "AWS_REGION",                       value = var.aws_region },
      { name = "S3_BUCKET_NAME",                   value = var.s3_media_bucket_name },
      { name = "ALLOWED_ORIGINS",                  value = "https://${var.domain_name}" },
      { name = "DATABASE_URL",                     value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${var.db_name}" },
      { name = "DATABASE_USERNAME",                value = var.db_username },
      { name = "DATABASE_PASSWORD",                value = var.db_password },
      { name = "JWT_SECRET",                       value = var.jwt_secret },
      { name = "ACCESS_TOKEN_EXPIRE_MINUTES",      value = "60" },
      { name = "DEVICE_TOKEN_EXPIRE_DAYS",         value = "365" },
      { name = "PRESIGNED_UPLOAD_EXPIRE_SECONDS",  value = "900" },
      { name = "PRESIGNED_DOWNLOAD_EXPIRE_SECONDS", value = "86400" },
      { name = "ALLOWED_IMAGE_TYPES",              value = "image/jpeg,image/png,image/webp,image/gif" },
      { name = "ALLOWED_VIDEO_TYPES",              value = "video/mp4,video/webm" },
      { name = "IMPRESSION_BATCH_SIZE",            value = "500" }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/cms"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = { Name = "cms-task" }
}

resource "aws_ecs_service" "main" {
  name            = "cms-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.main.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = "cms-backend"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.https]

  tags = { Name = "cms-service" }
}
