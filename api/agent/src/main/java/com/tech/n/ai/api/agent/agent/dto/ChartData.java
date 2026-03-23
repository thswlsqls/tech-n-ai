package com.tech.n.ai.api.agent.agent.dto;

import java.util.List;

/**
 * 프론트엔드 차트 렌더링용 구조화 데이터
 *
 * <p>통계 집계와 키워드 빈도 분석 결과를 통일된 포맷으로 전달한다.
 * 프론트엔드 차트 라이브러리(Recharts 등)에서 dataPoints를 직접
 * data prop으로 전달할 수 있도록 label/value 쌍으로 구성한다.
 */
public record ChartData(
    String chartType,
    String title,
    ChartMeta meta,
    List<DataPoint> dataPoints
) {
    public record ChartMeta(
        String groupBy,
        String startDate,
        String endDate,
        long totalCount
    ) {}

    public record DataPoint(
        String label,
        long value
    ) {}
}
