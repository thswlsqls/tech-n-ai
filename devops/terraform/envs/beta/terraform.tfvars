project     = "techai"
environment = "beta"
region      = "ap-northeast-2"
vpc_cidr    = "10.20.0.0/16"

# 네트워크 — single NAT (비용 절감)
single_nat_gateway   = true
enable_vpc_endpoints = true

# Aurora Serverless v2 — dev 보다 넓은 ACU
aurora_engine_mode             = "serverlessv2"
aurora_min_acu                 = 0.5
aurora_max_acu                 = 4.0
aurora_backup_retention_period = 7
aurora_deletion_protection     = false
aurora_skip_final_snapshot     = true

# ElastiCache — Multi-AZ
cache_node_type                = "cache.t4g.small"
cache_replicas_per_node_group  = 1
cache_multi_az_enabled         = true
cache_snapshot_retention_limit = 3

# MSK Serverless — beta 통합 테스트
enable_msk          = true
use_msk_provisioned = false

# ECS — 1~2 task
ecs_desired_count         = 1
ecs_autoscaling_min_count = 1
ecs_autoscaling_max_count = 4
