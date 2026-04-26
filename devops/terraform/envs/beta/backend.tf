terraform {
  backend "s3" {
    key            = "envs/beta/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "techai-tflock"
    encrypt        = true
    kms_key_id     = "alias/techai/tfstate"
  }
}
