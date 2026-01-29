# Emerging Tech API HTTP 테스트 파일

이 디렉토리는 **IntelliJ IDEA 내장 HTTP Client**를 사용하여 Emerging Tech API를 테스트하기 위한 `.http` 파일들을 포함하고 있습니다.

별도의 Postman이나 다른 도구 없이 **IDE 내에서 바로 API 테스트**가 가능합니다.

## 파일 구조

```
http/
├── README.md                           # 이 파일
├── http-client.env.json                # 환경별 공개 변수 (baseUrl, gatewayUrl 등)
├── http-client.private.env.json        # 환경별 비공개 변수 - Git 제외
├── .gitignore                          # Git 제외 파일 목록
├── 01-emerging-tech-list.http          # 목록 조회 API 테스트
├── 02-emerging-tech-detail.http        # 상세 조회 API 테스트
└── 03-emerging-tech-search.http        # 검색 API 테스트
```

## IntelliJ HTTP Client 사용 방법

### 1. 시작하기

1. IntelliJ IDEA에서 `.http` 파일 열기
2. 파일 상단의 환경 선택 드롭다운에서 `local`, `dev`, `prod` 중 선택
3. 각 요청 옆의 **실행 버튼** 클릭 또는 `Ctrl+Enter` (Mac: `Cmd+Enter`)

### 2. 환경 설정

환경별 변수는 `http-client.env.json` 파일에서 관리됩니다:

```json
{
  "local": {
    "baseUrl": "http://localhost:8087",
    "gatewayUrl": "http://localhost:8081"
  }
}
```

### 3. 테스트 순서

#### 기본 플로우 (권장)
1. **01-emerging-tech-list.http** - 목록 조회 및 테스트용 ID 확보
2. **02-emerging-tech-detail.http** - 상세 조회 (ID 필요)
3. **03-emerging-tech-search.http** - 키워드 검색

## API 엔드포인트 목록

| 파일 | 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|------|--------|-----------|------|----------|
| 01-emerging-tech-list.http | GET | `/api/v1/emerging-tech` | 목록 조회 (필터/정렬/페이지네이션) | X |
| 02-emerging-tech-detail.http | GET | `/api/v1/emerging-tech/{id}` | 상세 조회 | X |
| 03-emerging-tech-search.http | GET | `/api/v1/emerging-tech/search` | 키워드 검색 | X |

## 테스트 케이스

### 01-emerging-tech-list.http (목록 조회) - 16개
- 기본 목록 조회 및 페이지네이션 응답 확인
- 페이지네이션 (두 번째 페이지, 큰 페이지, 최소/최대 크기)
- 필터 (provider, updateType, status, 복합 필터)
- 정렬 (publishedAt asc, createdAt desc)
- 엣지 케이스 (page=0, size=0, size=101, 잘못된 provider, 파라미터 없음)

### 02-emerging-tech-detail.http (상세 조회) - 7개
- 사전 작업 (목록에서 ID 가져오기)
- 상세 조회 및 필드 검증
- 동일 ID 재조회 (일관성 확인)
- 실패 케이스 (존재하지 않는 ID, 잘못된 형식, 빈 ID, 특수문자)

### 03-emerging-tech-search.http (검색) - 11개
- 기본 검색 (영문/한글 키워드)
- 페이지네이션, 빈 결과, 대소문자 무시, 긴 검색어
- 실패 케이스 (검색어 누락, 빈 검색어, 공백 검색어)
- 엣지 케이스 (page=0, size=101)

## 요청/응답 형식

### EmergingTechListResponse (목록 응답)
```json
{
  "success": true,
  "data": {
    "pageSize": 20,
    "pageNumber": 1,
    "totalCount": 100,
    "items": [{ "id": "...", "provider": "GITHUB", ... }]
  }
}
```

### EmergingTechDetailResponse (상세 응답)
```json
{
  "success": true,
  "data": {
    "id": "...",
    "provider": "GITHUB",
    "updateType": "RELEASE",
    "title": "제목",
    "summary": "요약",
    "url": "https://...",
    "publishedAt": "2024-01-15T10:30:00",
    "sourceType": "BLOG",
    "status": "PUBLISHED",
    "externalId": "ext-123",
    "metadata": {
      "version": "1.0.0",
      "tags": ["tag1"],
      "author": "작성자",
      "githubRepo": "org/repo",
      "additionalInfo": {}
    },
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

## 페이지네이션 파라미터

| 파라미터 | 기본값 | 범위 | 설명 |
|----------|--------|------|------|
| page | 1 | min=1 | 페이지 번호 |
| size | 20 | 1~100 | 페이지 크기 |

## 필터 파라미터 (목록 조회)

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| provider | 기술 제공자 | GITHUB |
| updateType | 업데이트 유형 | RELEASE |
| status | 게시 상태 | PUBLISHED, PENDING, REJECTED |
| sort | 정렬 | publishedAt,desc (기본값) |

## 체크리스트

시작하기 전에 확인하세요:

- [ ] IntelliJ IDEA (Community 또는 Ultimate) 설치
- [ ] Emerging Tech API 서버가 실행 중 (`http://localhost:8087`)
- [ ] Gateway 서버가 실행 중 (`http://localhost:8081`) - 선택
- [ ] 환경 선택 (local, dev, prod)
- [ ] 첫 번째 요청 실행 테스트

## 참고 자료

- [IntelliJ HTTP Client 공식 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [HTTP 요청 응답 핸들러 스크립트](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#response-handler-scripts)
- [환경 변수 설정 방법](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#environment-variables)
