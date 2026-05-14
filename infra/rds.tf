resource "aws_db_subnet_group" "main" {
  name       = "cms-db-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = { Name = "cms-db-subnet-group" }
}

resource "aws_db_instance" "postgres" {
  identifier        = "cms-db"
  engine            = "postgres"
  engine_version    = "15.17"
  instance_class    = "db.t4g.micro"
  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  multi_az               = false
  publicly_accessible    = false
  skip_final_snapshot    = true
  deletion_protection    = false
  backup_retention_period = 0

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  tags = { Name = "cms-db" }
}
