# Step 12: langchain4j를 활용한 RAG 기반 챗봇 구현

## 개요

이 단계는 langchain4j 오픈소스를 활용하여 MongoDB Atlas Vector Search 기반의 RAG(Retrieval-Augmented Generation) 챗봇 시스템을 구현합니다.

## 관련 설계서

- `rag-chatbot-design.md`: RAG 챗봇 설계서

## 주요 내용

- langchain4j 오픈소스 통합
- MongoDB Atlas Vector Search 설정 및 최적화
- 멀티턴 대화 히스토리 관리
- Provider별 메시지 포맷 변환 (OpenAI, Anthropic)
- RAG 파이프라인 구현
- 토큰 제어 및 비용 통제 전략

## 의존성

- 11단계: CQRS 패턴 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수
- 12단계: 사용자 아카이브 기능 구현 완료 권장
