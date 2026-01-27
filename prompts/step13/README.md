# Step 13: langchain4j를 활용한 RAG 기반 챗봇 구현

## Plan Task

```
plan task: langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 구현
```

## 개요

`api-chatbot` 모듈의 RAG(Retrieval-Augmented Generation) 기반 챗봇 시스템을 구현합니다. langchain4j 오픈소스를 활용하여 MongoDB Atlas Vector Search 기반의 지식 검색 챗봇을 구축합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 13단계 섹션 참고
- `rag-chatbot-design-prompt.md`: RAG 챗봇 설계 프롬프트
- `user-archive-feature-design-prompt.md`: 사용자 아카이브 기능 설계 프롬프트

### 설계서
- `docs/step12/rag-chatbot-design.md`: RAG 챗봇 설계서
- `docs/step13/user-archive-feature-design.md`: 사용자 아카이브 기능 설계서

## 주요 작업 내용

- `api-chatbot` 모듈 구현 (langchain4j 통합, MongoDB Atlas Vector Search 설정)
- LLM Provider 및 Embedding Model 설정
- 멀티턴 대화 히스토리 관리
- Provider별 메시지 포맷 변환
- JWT 토큰 기반 인증 통합
- RAG 파이프라인 구현

## 의존성

- 11단계: CQRS 패턴 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수
- 12단계: 사용자 아카이브 기능 구현 완료 권장

## 다음 단계

- 14단계 (API Gateway 서버 구현)
