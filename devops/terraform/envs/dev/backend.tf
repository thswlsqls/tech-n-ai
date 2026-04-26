# 부트스트랩이 만든 state 인프라 사용
# bootstrap/outputs.tf 의 state_bucket_name / state_kms_alias / lock_table_name 참조
#
# 첫 init: terraform init -backend-config=backend.hcl
# 또는 아래 값을 직접 박아도 됨 (계정/리전 변경 시 backend.hcl 만 수정).

terraform {
  backend "s3" {
    # bucket 은 환경별 다를 수 있으니 -backend-config 로 주입을 권장.
    # 예) bucket = "techai-tfstate-111122223333-apne2"
    key            = "envs/dev/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "techai-tflock"
    encrypt        = true
    kms_key_id     = "alias/techai/tfstate"
  }
}
