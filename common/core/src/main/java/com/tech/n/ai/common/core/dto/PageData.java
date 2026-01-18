package com.tech.n.ai.common.core.dto;

import java.util.List;

/**
 * 페이징 데이터 객체
 * 페이징이 필요한 리스트 응답에 사용
 * 
 * @param <T> 리스트 항목 타입
 * @param pageSize 페이지 크기
 * @param pageNumber 현재 페이지 번호 (1부터 시작)
 * @param totalPageNumber 전체 페이지 수
 * @param totalSize 전체 데이터 수
 * @param list 데이터 리스트 배열
 */
public record PageData<T>(
    Integer pageSize,
    Integer pageNumber,
    Integer totalPageNumber,
    Integer totalSize,
    List<T> list
) {
    /**
     * 페이징 데이터 생성
     * 
     * @param pageSize 페이지 크기
     * @param pageNumber 현재 페이지 번호
     * @param totalSize 전체 데이터 수
     * @param list 데이터 리스트
     * @return 페이징 데이터
     */
    public static <T> PageData<T> of(Integer pageSize
                                   , Integer pageNumber
                                   , Integer totalSize
                                   , List<T> list) {
        Integer totalPageNumber = (totalSize + pageSize - 1) / pageSize; // 올림 계산
        return new PageData<>(pageSize, pageNumber, totalPageNumber, totalSize, list);
    }
}

