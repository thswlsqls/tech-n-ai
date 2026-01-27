package com.tech.n.ai.api.agent.tool.dto;

/**
 * Tool 실행 결과 DTO
 */
public record ToolResult(
    boolean success,
    String message,
    Object data
) {
    public static ToolResult success(String message) {
        return new ToolResult(true, message, null);
    }

    public static ToolResult success(String message, Object data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult failure(String message) {
        return new ToolResult(false, message, null);
    }
}
