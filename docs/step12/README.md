# Step 12: 사용자 북마크 기능 구현

## Plan Task

```
plan task: 사용자 북마크 기능 구현 (CQRS 패턴 적용)
```

## 개요

`api-bookmark` 모듈의 사용자 북마크 기능을 구현합니다. 로그인한 사용자가 조회할 수 있는 모든 contest, news 정보를 개인 북마크에 저장하고, 태그와 메모를 수정하며, 삭제 및 복구할 수 있는 기능을 제공합니다. 또한 태그와 메모를 기준으로 검색하고, 원본 아이템 정보를 기준으로 정렬할 수 있는 기능을 포함합니다.

## 작업 목표

- `api-bookmark` 모듈의 11개 API 엔드포인트 구현
- BookmarkCommandService, BookmarkQueryService, BookmarkHistoryService 구현
- BookmarkFacade 및 BookmarkController 구현
- DTO 및 예외 처리 구현
- Domain 모듈 확장 (BookmarkDocument 스키마 확장)

## 관련 설계서

- `user-bookmark-feature-design.md`: 사용자 북마크 기능 설계서

## 의존성

- 5단계: 사용자 인증 시스템 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 11단계: CQRS 패턴 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수

## 다음 단계

- 13단계 (RAG 기반 챗봇 구현)
