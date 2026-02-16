package org.example.exception;

import java.time.LocalDateTime;

/**
 * 统一错误响应格式
 * 
 * 用于API返回标准化的错误信息
 */
public record ErrorResponse(
        /**
         * HTTP状态码
         */
        int status,
        
        /**
         * 错误类型
         */
        String error,
        
        /**
         * 错误描述信息
         */
        String message,
        
        /**
         * 请求路径
         */
        String path,
        
        /**
         * 错误发生时间
         */
        LocalDateTime timestamp
) {
    
    /**
     * 创建错误响应
     * 
     * @param status HTTP状态码
     * @param error 错误类型
     * @param message 错误描述
     * @param path 请求路径
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now());
    }
}
