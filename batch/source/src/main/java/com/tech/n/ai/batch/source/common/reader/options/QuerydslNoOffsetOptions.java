package com.tech.n.ai.batch.source.common.reader.options;

import com.tech.n.ai.batch.source.common.reader.expression.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.annotation.Nonnull;
import java.lang.reflect.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class QuerydslNoOffsetOptions<T> {
    protected Log logger = LogFactory.getLog(getClass());

    protected final String fieldName;
    protected final Expression expression;

    public QuerydslNoOffsetOptions(@Nonnull Path field,
                                   @Nonnull Expression expression) {
        String[] qField = field.toString().split("\\.");
        this.fieldName = qField[qField.length-1];
        this.expression = expression;

        if (logger.isDebugEnabled()) {
            logger.debug("fieldName= " + fieldName);
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public abstract void initKeys(JPAQuery<T> query, int page);

    protected abstract void initFirstId(JPAQuery<T> query);
    protected abstract void initLastId(JPAQuery<T> query);

    public abstract JPAQuery<T> createQuery(JPAQuery<T> query, int page);

    public abstract void resetCurrentId(T item);

    protected Object getFiledValue(T item) {
        try {
            Field field = item.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(item);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Not Found or Not Access Field= " + fieldName, e);
            throw new IllegalArgumentException("Not Found or Not Access Field");
        }
    }

    public boolean isGroupByQuery(JPAQuery<T> query) {
        return isGroupByQuery(query.toString());
    }

    public boolean isGroupByQuery(String sql) {
        return sql.contains("group by");

    }

}
