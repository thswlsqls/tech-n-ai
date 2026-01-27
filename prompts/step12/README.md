# Step 12: 사용자 아카이브 기능 구현

## Plan Task

```
plan task: 사용자 아카이브 기능 구현 (CQRS 패턴 적용)
```

## 개요

`api-archive` 모듈의 사용자 아카이브 기능을 구현합니다. 로그인한 사용자가 조회할 수 있는 모든 contest, news 정보를 개인 아카이브에 저장하고, 태그와 메모를 수정하며, 삭제 및 복구할 수 있는 기능을 제공합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 12단계 섹션 참고
- `rag-chatbot-design-prompt.md`: RAG 챗봇 설계 프롬프트
- `rag-chatbot-multiturn-history-prompt.md`: RAG 챗봇 멀티턴 히스토리 프롬프트

### 설계서
- `docs/step12/rag-chatbot-design.md`: RAG 챗봇 설계서

## 주요 작업 내용

- `api-archive` 모듈의 11개 API 엔드포인트 구현
- ArchiveCommandService, ArchiveQueryService, ArchiveHistoryService 구현
- ArchiveFacade 및 ArchiveController 구현
- Domain 모듈 확장

## 의존성

- 5단계: 사용자 인증 시스템 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 11단계: CQRS 패턴 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수

## 다음 단계

- 13단계 (RAG 기반 챗봇 구현)
