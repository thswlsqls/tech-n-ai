package com.tech.n.ai.datasource.mongodb.document;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ExceptionLogDocument
 */
@Document(collection = "exception_logs")
@Getter
@Setter
public class ExceptionLogDocument {

    @Id
    private ObjectId id;

    @Field("source")
    private String source;

    @Field("exception_type")
    private String exceptionType;

    @Field("exception_message")
    private String exceptionMessage;

    @Field("stack_trace")
    private String stackTrace;

    @Field("context")
    private ExceptionContext context;

    @Field("occurred_at")
    private LocalDateTime occurredAt;

    @Field("severity")
    private String severity;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("updated_by")
    private String updatedBy;

    /**
     * ExceptionContext
     */
    @Getter
    @Setter
    public static class ExceptionContext {
        @Field("module")
        private String module;

        @Field("method")
        private String method;

        @Field("parameters")
        private Map<String, Object> parameters;

        @Field("user_id")
        private String userId;

        @Field("request_id")
        private String requestId;
    }
}
