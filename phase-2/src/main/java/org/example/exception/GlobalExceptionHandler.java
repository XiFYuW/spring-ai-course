package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

/**
 * 全局异常处理器
 * 
 * 统一处理所有Controller抛出的异常，返回标准化的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理非法参数异常
     * 
     * @param ex 异常
     * @param exchange 请求交换对象
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            ServerWebExchange exchange) {
        
        log.warn("参数错误: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * 处理聊天业务异常
     * 
     * @param ex 异常
     * @param exchange 请求交换对象
     * @return 400 Bad Request
     */
    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ErrorResponse> handleChatException(
            ChatException ex, 
            ServerWebExchange exchange) {
        
        log.warn("业务错误: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * 处理资源不存在异常
     * 
     * @param ex 异常
     * @param exchange 请求交换对象
     * @return 404 Not Found
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, 
            ServerWebExchange exchange) {
        
        // 判断是否是会话不存在的错误
        if (ex.getMessage() != null && ex.getMessage().contains("会话不存在")) {
            log.warn("资源不存在: {}", ex.getMessage());
            
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    ex.getMessage(),
                    exchange.getRequest().getPath().value()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // 其他运行时异常作为500处理
        log.error("服务器错误: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "服务器内部错误",
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * 处理所有其他未捕获的异常
     * 
     * @param ex 异常
     * @param exchange 请求交换对象
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, 
            ServerWebExchange exchange) {
        
        log.error("未预期的错误: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "服务器内部错误",
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
