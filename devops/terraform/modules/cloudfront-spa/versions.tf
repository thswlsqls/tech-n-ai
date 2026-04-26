terraform {
  required_version = "~> 1.9.5"

  required_providers {
    aws = {
      source                = "hashicorp/aws"
      version               = "~> 5.60"
      configuration_aliases = [aws.us_east_1]   # ACM·WAF CloudFront scope 는 us-east-1
    }
  }
}
