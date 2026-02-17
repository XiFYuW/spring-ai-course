package org.example.controller;

import org.example.entity.ConversationMessage;
import org.example.entity.ConversationSession;
import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 聊天控制器 - RESTful API
 * 
 * API 设计原则：
 * 1. 所有聊天操作都需要提供 sessionId，明确指定操作哪个会话
 * 2. 会话管理与会话操作分离
 * 3. 支持流式和非流式两种聊天模式
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 普通聊天（非流式）
     * 
     * @param request 包含 sessionId、message 和可选的 roleId
     * @return AI 回复
     */
    @PostMapping
    public Mono<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.sessionId(), request.message(), request.roleId());
    }

    /**
     * 流式聊天（SSE）
     * 
     * @param request 包含 sessionId、message 和可选的 roleId
     * @return 流式 AI 回复
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.sessionId(), request.message(), request.roleId());
    }

    // ==================== 会话管理 API ====================

    /**
     * 创建新会话
     * 
     * @param request 可选的会话标题
     * @return 新创建的会话ID
     */
    @PostMapping("/sessions")
    public Mono<CreateSessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String title = (request != null) ? request.title() : null;
        return chatService.createNewSession(title)
                .map(CreateSessionResponse::new);
    }

    /**
     * 获取所有会话列表
     * 
     * @return 按更新时间倒序排列的会话列表
     */
    @GetMapping("/sessions")
    public Flux<ConversationSession> getAllSessions() {
        return chatService.getAllSessions();
    }

    /**
     * 获取指定会话的详细信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ConversationSession> getSession(@PathVariable Long sessionId) {
        return chatService.getSession(sessionId);
    }

    /**
     * 获取指定会话的所有消息
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Mono<List<ConversationMessage>> getSessionMessages(@PathVariable Long sessionId) {
        return chatService.getSessionMessages(sessionId).collectList();
    }

    /**
     * 删除会话及其所有消息
     * 
     * @param sessionId 要删除的会话ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<Void> deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId);
    }

    // ==================== 请求/响应记录 ====================

    /**
     * 聊天请求
     * 
     * @param sessionId 会话ID（必填）
     * @param message 用户消息
     * @param roleId 选择的角色ID（可选，如果为空则使用默认角色）
     */
    public record ChatRequest(Long sessionId, String message, Long roleId) {}

    /**
     * 创建会话请求
     * 
     * @param title 会话标题（可选）
     */
    public record CreateSessionRequest(String title) {}

    /**
     * 创建会话响应
     * 
     * @param sessionId 新创建的会话ID
     */
    public record CreateSessionResponse(Long sessionId) {}
}
