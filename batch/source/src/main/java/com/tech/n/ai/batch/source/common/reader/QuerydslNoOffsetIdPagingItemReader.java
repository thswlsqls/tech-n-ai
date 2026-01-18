package com.tech.n.ai.batch.source.common.reader;

import com.tech.n.ai.batch.source.common.reader.options.QuerydslNoOffsetNumberOptions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.util.function.Function;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

public class QuerydslNoOffsetIdPagingItemReader<T, N extends Number & Comparable<?>> extends
    QuerydslPagingItemReader<T> {

    private QuerydslNoOffsetNumberOptions<T, N> options;

    private QuerydslNoOffsetIdPagingItemReader() {
        super();
        setName(ClassUtils.getShortName(QuerydslNoOffsetIdPagingItemReader.class));
    }

    public QuerydslNoOffsetIdPagingItemReader(EntityManagerFactory entityManagerFactory,
                                              int pageSize,
                                              QuerydslNoOffsetNumberOptions<T, N> options,
                                              Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        super(entityManagerFactory, pageSize, queryFunction);
        setName(ClassUtils.getShortName(QuerydslNoOffsetIdPagingItemReader.class));
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        EntityTransaction tx = getTxOrNull();

        JPQLQuery<T> query = createQuery().limit(getPageSize());

        initResults();

        fetchQuery(query, tx);

        resetCurrentIdIfNotLastPage();
    }

    @Override
    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        JPAQuery<T> query = queryFunction.apply(queryFactory);
        options.initKeys(query, getPage()); // 제일 첫번째 페이징시 시작해야할 ID 찾기

        return options.createQuery(query, getPage());
    }

    private void resetCurrentIdIfNotLastPage() {
        if (isNotEmptyResults()) {
            options.resetCurrentId(getLastItem());
        }
    }

    // 조회결과가 Empty이면 results에 null이 담긴다
    private boolean isNotEmptyResults() {
        return !CollectionUtils.isEmpty(results) && results.get(0) != null;
    }

    private T getLastItem() {
        return results.get(results.size() - 1);
    }
}
