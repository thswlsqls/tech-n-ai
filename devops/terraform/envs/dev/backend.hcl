# terraform init -backend-config=backend.hcl 로 주입되는 동적 backend 설정
# bucket 은 부트스트랩 출력(state_bucket_name)을 그대로 사용.
# 본 파일은 git 에 커밋해도 됨 — 비밀 정보 없음.

bucket = "techai-tfstate-PLACEHOLDER-ACCOUNT-ID-apne2"
