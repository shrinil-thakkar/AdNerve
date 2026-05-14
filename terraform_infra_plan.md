# CMS Pilot — Terraform Infrastructure
### Claude Code Document | Apply after manual Steps 1–3 + S3 state bucket are complete

---

## Context

This document contains everything Claude needs to write the complete Terraform configuration
for the CMS backend pilot infrastructure on AWS.

### What has already been done manually (do NOT recreate these)
- IAM admin user: `cms-admin` with `AdministratorAccess`
- ECR repository URI: `872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend`
- S3 bucket for Terraform state: `cms-terraform-state-872709212110`
- AWS region: `ap-south-1` (Mumbai)

### What Terraform must create
- VPC, subnets, security groups, routing
- RDS PostgreSQL 15 (db.t4g.micro, single-AZ, private subnet)
- S3 media bucket (private, encrypted, CORS, lifecycle)
- IAM roles for ECS execution and task
- ALB with HTTP→HTTPS redirect, ACM certificate (DNS validation)
- ECS Fargate cluster + task definition + service
- CloudWatch log group

---

## Account Details

| Field | Value |
|-------|-------|
| AWS Account ID | `872709212110` |
| Region | `ap-south-1` |
| ECR image URI | `872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest` |
| Terraform state bucket | `cms-terraform-state-872709212110` |
| App name prefix | `cms` |
| ECS container port | `8080` |
| Health check path | `/actuator/health` |

---

## Repository Layout to Create

```
infra/
├── main.tf            # Provider, Terraform backend (S3 state)
├── variables.tf       # All input variables with descriptions
├── outputs.tf         # ALB DNS, RDS endpoint, ACM validation records
├── terraform.tfvars.example   # Template — never commit real values
├── .gitignore
├── vpc.tf             # VPC, subnets, IGW, NAT, route tables, security groups
├── iam.tf             # ECS execution role, ECS task role (S3 access)
├── s3.tf              # Media bucket (public access blocked, CORS, lifecycle)
├── rds.tf             # PostgreSQL RDS instance + subnet group
├── alb.tf             # ALB, target group, HTTP listener, HTTPS listener, ACM cert
└── ecs.tf             # ECS cluster, task definition, service, CloudWatch log group
```

---

## `main.tf`

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "cms-terraform-state-872709212110"
    key    = "pilot/terraform.tfstate"
    region = "ap-south-1"
  }
}

provider "aws" {
  region = var.aws_region
}
```

---

## `variables.tf`

Define all of these variables. All sensitive ones must be marked `sensitive = true`.

| Variable | Type | Default | Sensitive | Description |
|----------|------|---------|-----------|-------------|
| `aws_region` | string | `"ap-south-1"` | no | AWS region |
| `app_name` | string | `"cms"` | no | Prefix for all resource names |
| `ecr_image_uri` | string | `"872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest"` | no | Full ECR image URI |
| `s3_media_bucket_name` | string | — | no | Globally unique S3 bucket name for media files |
| `domain_name` | string | — | no | Full subdomain for the API e.g. `api.yourdomain.com` |
| `db_password` | string | — | yes | RDS PostgreSQL password |
| `jwt_secret` | string | — | yes | JWT signing secret (min 32 chars) |
| `db_username` | string | `"cms_user"` | no | RDS master username |
| `db_name` | string | `"cms_db"` | no | RDS database name |
| `container_port` | number | `8080` | no | Port the Spring Boot app listens on |
| `task_cpu` | number | `512` | no | Fargate task CPU units (512 = 0.5 vCPU) |
| `task_memory` | number | `1024` | no | Fargate task memory in MB |

---

## `outputs.tf`

Output these values after `terraform apply`:

| Output | Description |
|--------|-------------|
| `alb_dns_name` | DNS name of the ALB — use this for the domain A/ALIAS record |
| `db_endpoint` | RDS endpoint — use this to verify DB connectivity |
| `acm_validation_records` | CNAME records to add to DNS to validate the ACM certificate |
| `ecs_cluster_name` | ECS cluster name — used in deploy commands |
| `ecs_service_name` | ECS service name — used in deploy commands |
| `s3_media_bucket_name` | Confirmed S3 media bucket name |

---

## `vpc.tf` — Networking

### VPC
- CIDR: `10.0.0.0/16`
- `enable_dns_support = true`
- `enable_dns_hostnames = true`
- Tag: `Name = "cms-vpc"`

### Subnets
Create 4 subnets across 2 AZs:

| Name | CIDR | AZ | Type | Purpose |
|------|------|----|------|---------|
| `cms-public-a` | `10.0.1.0/24` | `ap-south-1a` | Public | ALB |
| `cms-public-b` | `10.0.2.0/24` | `ap-south-1b` | Public | ALB (HA requirement) |
| `cms-private-a` | `10.0.3.0/24` | `ap-south-1a` | Private | ECS tasks + RDS |
| `cms-private-b` | `10.0.4.0/24` | `ap-south-1b` | Private | ECS tasks + RDS |

Public subnets: `map_public_ip_on_launch = true`

### Internet Gateway
One IGW attached to the VPC. Tag: `Name = "cms-igw"`

### NAT Gateway
One NAT Gateway in `cms-public-a`.
Requires one Elastic IP (`domain = "vpc"`).
Tag: `Name = "cms-nat"`

> NAT Gateway is needed so ECS tasks in private subnets can pull Docker images
> from ECR and make outbound HTTPS calls to S3.

### Route Tables

**Public route table:**
- Route: `0.0.0.0/0` → Internet Gateway
- Associated with: `cms-public-a`, `cms-public-b`

**Private route table:**
- Route: `0.0.0.0/0` → NAT Gateway
- Associated with: `cms-private-a`, `cms-private-b`

### Security Groups

**`cms-alb-sg`** (attached to ALB):
- Inbound: TCP 80 from `0.0.0.0/0` (HTTP, will redirect to 443)
- Inbound: TCP 443 from `0.0.0.0/0` (HTTPS)
- Outbound: all traffic to `0.0.0.0/0`

**`cms-ecs-sg`** (attached to ECS tasks):
- Inbound: TCP `var.container_port` from `cms-alb-sg` only (not from internet)
- Outbound: all traffic to `0.0.0.0/0`

**`cms-rds-sg`** (attached to RDS):
- Inbound: TCP 5432 from `cms-ecs-sg` only (not from internet)
- No outbound rule needed (RDS doesn't initiate connections)

---

## `iam.tf` — IAM Roles

### ECS Task Execution Role
Name: `cms-ecs-execution-role`

Trust policy: allow `ecs-tasks.amazonaws.com` to assume this role.

Attach managed policy: `arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy`

This allows ECS to:
- Pull the container image from ECR
- Write logs to CloudWatch

### ECS Task Role
Name: `cms-ecs-task-role`

Trust policy: allow `ecs-tasks.amazonaws.com` to assume this role.

Attach an inline policy named `cms-task-s3-policy` that allows the running app to:
```
s3:PutObject
s3:GetObject
s3:DeleteObject
s3:ListBucket
```
On resources:
- `aws_s3_bucket.media.arn`
- `aws_s3_bucket.media.arn/*`

> The execution role is used by the ECS agent (to start the container).
> The task role is used by the running application code (to access S3).
> These must be two separate roles.

---

## `s3.tf` — Media Bucket

Bucket name: `var.s3_media_bucket_name`

### Required configurations (all as separate resources, not inline blocks):

**Public access block** — all four options set to `true`:
- `block_public_acls`
- `block_public_policy`
- `ignore_public_acls`
- `restrict_public_buckets`

**Server-side encryption:**
- Algorithm: `AES256`

**CORS configuration:**
- One rule:
  - `allowed_methods`: `["PUT"]`
  - `allowed_origins`: `["https://${var.domain_name}"]`
  - `allowed_headers`: `["*"]`
  - `max_age_seconds`: `3000`

**Lifecycle configuration:**
- Rule ID: `intelligent-tiering`
- Status: `Enabled`
- Transition after 90 days to `INTELLIGENT_TIERING` storage class

Set `force_destroy = false` on the bucket resource.

---

## `rds.tf` — PostgreSQL Database

### DB Subnet Group
Name: `cms-db-subnet-group`
Subnets: `cms-private-a` and `cms-private-b`

### DB Instance

| Setting | Value |
|---------|-------|
| Identifier | `cms-db` |
| Engine | `postgres` |
| Engine version | `15.6` |
| Instance class | `db.t4g.micro` |
| Allocated storage | `20` GB |
| Storage type | `gp3` |
| DB name | `var.db_name` |
| Username | `var.db_username` |
| Password | `var.db_password` |
| Multi-AZ | `false` |
| Publicly accessible | `false` |
| Skip final snapshot | `true` |
| Deletion protection | `false` |
| Backup retention | `3` days |
| VPC security groups | `cms-rds-sg` |
| DB subnet group | the one created above |

---

## `alb.tf` — Load Balancer + HTTPS

### ACM Certificate
- Domain: `var.domain_name`
- Validation method: `DNS`
- Lifecycle: `create_before_destroy = true`

Output the `domain_validation_options` so the user knows which CNAME records to add to DNS.

### Application Load Balancer
- Name: `cms-alb`
- Internal: `false`
- Type: `application`
- Security groups: `cms-alb-sg`
- Subnets: `cms-public-a`, `cms-public-b`

### Target Group
- Name: `cms-tg`
- Port: `var.container_port`
- Protocol: `HTTP`
- Target type: `ip` (required for Fargate awsvpc networking)
- VPC: the one created in `vpc.tf`

Health check:
- Path: `/actuator/health`
- Protocol: `HTTP`
- Healthy threshold: `2`
- Unhealthy threshold: `3`
- Interval: `30` seconds
- Timeout: `5` seconds
- Matcher: `200`

### HTTP Listener (port 80)
Default action: redirect to HTTPS port 443, status code `HTTP_301`.

### HTTPS Listener (port 443)
- SSL policy: `ELBSecurityPolicy-TLS13-1-2-2021-06`
- Certificate: the ACM cert created above
- Default action: forward to target group

---

## `ecs.tf` — Fargate Compute

### CloudWatch Log Group
- Name: `/ecs/cms`
- Retention: `30` days

### ECS Cluster
- Name: `cms-cluster`

### ECS Task Definition

| Setting | Value |
|---------|-------|
| Family | `cms-task` |
| Network mode | `awsvpc` |
| Requires compatibilities | `["FARGATE"]` |
| CPU | `var.task_cpu` |
| Memory | `var.task_memory` |
| Execution role ARN | `cms-ecs-execution-role` ARN |
| Task role ARN | `cms-ecs-task-role` ARN |

Container definition (single container named `cms-backend`):

**Image:** `var.ecr_image_uri`

**Port mappings:** container port `var.container_port`, protocol `tcp`

**Environment variables** (passed as plain environment, not secrets for pilot):

| Name | Value |
|------|-------|
| `APP_ENV` | `production` |
| `AWS_REGION` | `var.aws_region` |
| `S3_BUCKET_NAME` | `var.s3_media_bucket_name` |
| `ALLOWED_ORIGINS` | `https://${var.domain_name}` |
| `DATABASE_URL` | `jdbc:postgresql://${rds_endpoint}/var.db_name` |
| `DATABASE_USERNAME` | `var.db_username` |
| `DATABASE_PASSWORD` | `var.db_password` |
| `JWT_SECRET` | `var.jwt_secret` |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | `60` |
| `DEVICE_TOKEN_EXPIRE_DAYS` | `365` |
| `PRESIGNED_UPLOAD_EXPIRE_SECONDS` | `900` |
| `PRESIGNED_DOWNLOAD_EXPIRE_SECONDS` | `86400` |
| `ALLOWED_IMAGE_TYPES` | `image/jpeg,image/png,image/webp,image/gif` |
| `ALLOWED_VIDEO_TYPES` | `video/mp4,video/webm` |
| `IMPRESSION_BATCH_SIZE` | `500` |

> Note on DATABASE_URL: The RDS endpoint from `aws_db_instance.postgres.endpoint`
> already includes the port (e.g. `cms-db.xxxx.ap-south-1.rds.amazonaws.com:5432`).
> The full JDBC URL should be:
> `"jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${var.db_name}"`

**Log configuration:**
```
logDriver: awslogs
options:
  awslogs-group:         /ecs/cms
  awslogs-region:        var.aws_region
  awslogs-stream-prefix: ecs
```

**Health check** (container-level, separate from ALB health check):
```
command:     ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
interval:    30
timeout:     5
retries:     3
startPeriod: 60   ← give JVM time to start before health checks begin
```

### ECS Service

| Setting | Value |
|---------|-------|
| Name | `cms-service` |
| Cluster | `cms-cluster` |
| Task definition | `cms-task` (latest revision) |
| Desired count | `1` |
| Launch type | `FARGATE` |
| Subnets | `cms-private-a`, `cms-private-b` |
| Security groups | `cms-ecs-sg` |
| Assign public IP | `false` |

Load balancer attachment:
- Target group: `cms-tg`
- Container name: `cms-backend`
- Container port: `var.container_port`

Add `depends_on = [aws_lb_listener.https]` to ensure the ALB listener exists before
the service starts registering targets.

---

## `.gitignore` for `infra/`

```
# Terraform state and sensitive files
.terraform/
.terraform.lock.hcl
*.tfstate
*.tfstate.backup
terraform.tfvars
*.tfvars
!*.tfvars.example

# OS
.DS_Store
```

---

## `terraform.tfvars.example`

Create this as a template. The user fills in real values in `terraform.tfvars` (which is gitignored).

```hcl
# Copy this file to terraform.tfvars and fill in real values.
# Never commit terraform.tfvars to git.

aws_region          = "ap-south-1"
app_name            = "cms"
ecr_image_uri       = "872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest"
s3_media_bucket_name = "cms-media-<your-unique-suffix>"   # must be globally unique
domain_name         = "api.yourdomain.com"
db_username         = "cms_user"
db_name             = "cms_db"
container_port      = 8080
task_cpu            = 512
task_memory         = 1024

# Sensitive — fill in real values
db_password         = ""   # min 8 chars, no special chars that break JDBC URLs
jwt_secret          = ""   # min 32 random characters
```

---

## Coding Rules for Claude

1. Use Terraform `~> 5.0` AWS provider
2. Use `aws_` resource names that match the `var.app_name` prefix (e.g. `aws_vpc.main`, `aws_subnet.public_a`)
3. All resource tags must include at minimum `{ Name = "..." }`
4. Security group rules must use `security_groups` references, not hardcoded CIDRs, wherever possible
5. Do not use `aws_security_group_rule` as separate resources — define ingress/egress inline in `aws_security_group`
6. Use `jsonencode()` for IAM policies and ECS container definitions — not heredoc strings
7. Reference all cross-resource values via Terraform resource attributes (e.g. `aws_vpc.main.id`) — never hardcode IDs
8. The RDS endpoint attribute `aws_db_instance.postgres.endpoint` already includes `:5432` — do not append the port again in the JDBC URL
9. ACM certificate must have `lifecycle { create_before_destroy = true }`
10. ECS service must have `depends_on = [aws_lb_listener.https]`
11. Output `acm_validation_records` as the full `domain_validation_options` so the user can see the CNAME records to add

---

## How to Apply After Claude Generates the Code

```bash
# 1. Install Terraform if not already done
brew install hashicorp/tap/terraform   # macOS

# 2. Copy example vars and fill in real values
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your domain, passwords, bucket name

# 3. Initialise (connects to S3 state backend)
cd infra
terraform init

# 4. Preview
terraform plan -var-file="terraform.tfvars"
# Review carefully — should show ~28-32 resources to create

# 5. Apply
terraform apply -var-file="terraform.tfvars"
# Type 'yes' when prompted
# Takes 10–15 minutes (RDS is the slowest)

# 6. Note the outputs
# alb_dns_name           → create A/ALIAS record in your DNS pointing to this
# acm_validation_records → create these CNAME records in your DNS to issue the certificate
# db_endpoint            → for reference
```

## After Applying — DNS Steps (Manual, not Terraform)

These two steps must be done manually in your domain registrar or Route 53 after `terraform apply`:

**Step A — Validate the ACM certificate:**
Add the CNAME record from `acm_validation_records` output to your DNS.
Certificate status in ACM console changes from "Pending validation" → "Issued" within 2–30 minutes.

**Step B — Point your domain at the ALB:**
Add an A record (or ALIAS if using Route 53) for your subdomain pointing to `alb_dns_name` output value.

The backend is fully live once both steps are complete and you can reach `https://api.yourdomain.com/actuator/health`.

---

## Redeployment Command (for reference after first deploy)

```bash
# Build and push new image
./gradlew bootJar
docker build -t cms-backend .
aws ecr get-login-password --region ap-south-1 \
  | docker login --username AWS --password-stdin \
    872709212110.dkr.ecr.ap-south-1.amazonaws.com
docker tag cms-backend:latest \
  872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest
docker push \
  872709212110.dkr.ecr.ap-south-1.amazonaws.com/cmsbackend:latest

# Force ECS to pull the new image
aws ecs update-service \
  --cluster cms-cluster \
  --service cms-service \
  --force-new-deployment \
  --region ap-south-1
```
