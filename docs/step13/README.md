# Step 13: langchain4j를 활용한 RAG 기반 챗봇 구현

## Plan Task

```
plan task: langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 구현
```

## 개요

`api-chatbot` 모듈의 RAG(Retrieval-Augmented Generation) 기반 챗봇 시스템을 구현합니다. langchain4j 오픈소스를 활용하여 MongoDB Atlas Vector Search 기반의 지식 검색 챗봇을 구축하며, ContestDocument, NewsArticleDocument, BookmarkDocument를 임베딩하여 벡터 검색 기반의 자연어 질의응답 시스템을 제공합니다.

## 작업 목표

- `api-chatbot` 모듈 구현 (langchain4j 통합, MongoDB Atlas Vector Search 설정)
- LLM Provider 및 Embedding Model 설정: OpenAI GPT-4o-mini (기본 LLM), OpenAI text-embedding-3-small (기본 Embedding)
- 멀티턴 대화 히스토리 관리 (세션 관리, 메시지 저장, ChatMemory 통합)
- Provider별 메시지 포맷 변환 (OpenAI 기본, Anthropic 대안)
- JWT 토큰 기반 인증 통합
- RAG 파이프라인 구현 (입력 전처리, 벡터 검색, 프롬프트 체인, 답변 생성)

## 관련 설계서

- `rag-chatbot-design.md`: RAG 챗봇 설계서

## 의존성

- 11단계: CQRS 패턴 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수
- 12단계: 사용자 북마크 기능 구현 완료 권장

## 다음 단계

- 14단계 (API Gateway 서버 구현)
