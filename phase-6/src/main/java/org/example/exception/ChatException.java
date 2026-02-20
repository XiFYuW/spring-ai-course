package org.example.exception;

/**
 * 聊天服务业务异常
 * 
 * 用于表示业务逻辑错误，如会话不存在、参数无效等
 */
public class ChatException extends RuntimeException {
    
    public ChatException(String message) {
        super(message);
    }
    
    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
