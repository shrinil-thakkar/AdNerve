output "alb_dns_name" {
  description = "DNS name of the ALB — use this for the domain A/ALIAS record"
  value       = aws_lb.main.dns_name
}

output "db_endpoint" {
  description = "RDS endpoint — use this to verify DB connectivity"
  value       = aws_db_instance.postgres.endpoint
}

output "acm_validation_records" {
  description = "CNAME records to add to DNS to validate the ACM certificate"
  value       = aws_acm_certificate.main.domain_validation_options
}

output "ecs_cluster_name" {
  description = "ECS cluster name — used in deploy commands"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name — used in deploy commands"
  value       = aws_ecs_service.main.name
}

output "s3_media_bucket_name" {
  description = "Confirmed S3 media bucket name"
  value       = aws_s3_bucket.media.bucket
}
