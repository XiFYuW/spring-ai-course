package org.example.service;

import org.example.config.ChatProperties;
import org.example.entity.ConversationMessage;
import org.example.entity.ConversationSession;
import org.example.entity.RoleConfig;
import org.example.repository.ConversationMessageRepository;
import org.example.repository.ConversationSessionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天服务 - 支持多会话管理和上下文长度控制
 * 
 * 1. 无状态设计 - 不保存当前会话状态，所有操作都需要明确的 sessionId
 * 2. 上下文控制 - 限制历史消息为20对（40条消息），防止token超限
 * 3. 响应式编程 - 全面使用 Mono/Flux 进行异步操作
 * 4. 配置外部化 - 系统提示词和上下文长度通过配置文件管理
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final ChatProperties chatProperties;
    private final RoleConfigService roleConfigService;

    public ChatService(
            ChatModel chatModel,
            ConversationSessionRepository sessionRepository,
            ConversationMessageRepository messageRepository,
            ChatProperties chatProperties,
            RoleConfigService roleConfigService
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatProperties = chatProperties;
        this.roleConfigService = roleConfigService;
    }

    /**
     * 普通聊天（非流式）
     * 
     * @param sessionId 会话ID，必须提供
     * @param userMessage 用户消息
     * @return AI回复
     */
    public Mono<String> chat(Long sessionId, String userMessage) {
        return chat(sessionId, userMessage, null);
    }

    /**
     * 普通聊天（非流式） - 支持角色选择
     * 
     * @param sessionId 会话ID，必须提供
     * @param userMessage 用户消息
     * @param roleId 选择的角色ID（可选，如果为空则使用默认角色）
     * @return AI回复
     */
    public Mono<String> chat(Long sessionId, String userMessage, Long roleId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
        }
        
        return validateSessionExists(sessionId)
                .flatMap(sid -> buildConversationHistory(sid, roleId)
                        .flatMap(history -> {
                            // 添加用户消息到历史
                            history.add(new UserMessage(userMessage));
                            
                            Prompt prompt = new Prompt(new ArrayList<>(history));
                            
                            return Mono.fromCallable(() -> 
                                    chatClient.prompt(prompt)
                                            .call()
                                            .content()
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(response -> {
                                if (response != null && !response.isEmpty()) {
                                    // 先保存用户消息，再保存AI回复
                                    return saveMessage(sid, MessageType.USER.getValue(), userMessage)
                                            .then(saveMessage(sid, MessageType.ASSISTANT.getValue(), response))
                                            .thenReturn(response);
                                }
                                return Mono.justOrEmpty(response);
                            })
                            .onErrorResume(TransientAiException.class, e -> {
                                String errorMsg = extractErrorMessage(e);
                                return Mono.just("【AI服务暂时不可用】" + errorMsg);
                            })
                            .onErrorResume(Exception.class, e -> 
                                Mono.just("【请求失败】" + e.getMessage())
                            );
                        })
                );
    }

    /**
     * 流式聊天（SSE）
     * 
     * @param sessionId 会话ID，必须提供
     * @param userMessage 用户消息
     * @return 流式AI回复
     */
    public Flux<String> chatStream(Long sessionId, String userMessage) {
        return chatStream(sessionId, userMessage, null);
    }

    /**
     * 流式聊天（SSE） - 支持角色选择
     * 
     * @param sessionId 会话ID，必须提供
     * @param userMessage 用户消息
     * @param roleId 选择的角色ID（可选，如果为空则使用默认角色）
     * @return 流式AI回复
     */
    public Flux<String> chatStream(Long sessionId, String userMessage, Long roleId) {
        if (sessionId == null) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
        }
        
        return validateSessionExists(sessionId)
                .flatMapMany(sid -> buildConversationHistory(sid, roleId)
                        .flatMapMany(history -> {
                            // 添加用户消息到历史
                            history.add(new UserMessage(userMessage));
                            
                            Prompt prompt = new Prompt(new ArrayList<>(history));
                            StringBuilder fullResponse = new StringBuilder();
                            
                            return chatClient.prompt(prompt)
                                    .stream()
                                    .content()
                                    .doOnNext(fullResponse::append)
                                    .doOnComplete(() -> {
                                        // 流完成后保存消息
                                        if (!fullResponse.isEmpty()) {
                                            saveMessage(sid, MessageType.USER.getValue(), userMessage)
                                                    .then(saveMessage(sid, MessageType.ASSISTANT.getValue(), fullResponse.toString()))
                                                    .subscribeOn(Schedulers.boundedElastic())
                                                    .subscribe();
                                        }
                                    })
                                    .onErrorResume(TransientAiException.class, e -> {
                                        String errorMsg = extractErrorMessage(e);
                                        return Flux.just("【AI服务暂时不可用】" + errorMsg);
                                    })
                                    .onErrorResume(Exception.class, e -> 
                                        Flux.just("【请求失败】" + e.getMessage())
                                    );
                        })
                );
    }

    /**
     * 创建新会话
     * 
     * @param title 会话标题
     * @return 新会话ID
     */
    public Mono<Long> createNewSession(String title) {
        String sessionTitle = (title == null || title.isBlank()) ? "新会话" : title;
        return sessionRepository.save(ConversationSession.create(sessionTitle))
                .map(ConversationSession::id);
    }

    /**
     * 获取会话的历史消息（用于恢复会话上下文）
     * 
     * @param sessionId 会话ID
     * @return 该会话的消息列表
     */
    public Flux<ConversationMessage> getSessionMessages(Long sessionId) {
        if (sessionId == null) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 获取所有会话列表（按更新时间倒序）
     * 
     * @return 会话列表
     */
    public Flux<ConversationSession> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * 删除会话及其所有消息
     * 
     * @param sessionId 要删除的会话ID
     * @return 操作完成信号
     */
    public Mono<Void> deleteSession(Long sessionId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        
        return messageRepository.deleteBySessionId(sessionId)
                .then(sessionRepository.deleteById(sessionId));
    }

    /**
     * 获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    public Mono<ConversationSession> getSession(Long sessionId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        return sessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new RuntimeException("会话不存在: " + sessionId)));
    }

    /**
     * 验证会话是否存在
     * 
     * @param sessionId 会话ID
     * @return 验证通过的会话ID
     */
    private Mono<Long> validateSessionExists(Long sessionId) {
        return sessionRepository.existsById(sessionId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(sessionId);
                    }
                    return Mono.error(new RuntimeException("会话不存在: " + sessionId));
                });
    }

    /**
     * 构建对话历史（带上下文长度控制和动态角色配置）
     * 
     * 策略：
     * 1. 使用动态角色配置构建系统消息
     * 2. 从数据库获取该会话的所有历史消息
     * 3. 如果消息数量超过限制，保留最近的 maxContextMessages 条
     * 4. 使用动态角色配置处理用户和助手消息
     * 
     * @param sessionId 会话ID
     * @return 构建好的消息列表（包含系统消息）
     */
    private Mono<List<Message>> buildConversationHistory(Long sessionId) {
        return buildConversationHistory(sessionId, null);
    }

    /**
     * 构建对话历史（带上下文长度控制和动态角色配置）- 支持角色选择
     * 
     * 策略：
     * 1. 根据选择的角色ID构建系统消息
     * 2. 如果没有选择角色，使用所有启用的角色配置
     * 3. 从数据库获取该会话的所有历史消息
     * 4. 如果消息数量超过限制，保留最近的 maxContextMessages 条
     * 
     * @param sessionId 会话ID
     * @param roleId 选择的角色ID（可选）
     * @return 构建好的消息列表（包含系统消息）
     */
    private Mono<List<Message>> buildConversationHistory(Long sessionId, Long roleId) {
        return buildDynamicRoleMessages(roleId)
                .flatMap(dynamicMessages -> 
                        messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                                .collectList()
                                .map(messages -> {
                                    List<Message> history = new ArrayList<>();
                                    
                                    // 1. 添加动态角色配置生成的消息
                                    history.addAll(dynamicMessages);
                                    
                                    // 2. 处理历史消息（上下文截断）
                                    List<ConversationMessage> contextMessages = messages;
                                    int maxContextMessages = chatProperties.getMaxContextMessages();
                                    
                                    // 如果消息过多，只保留最近的 maxContextMessages 条
                                    if (messages.size() > maxContextMessages) {
                                        // 保留后 maxContextMessages 条
                                        contextMessages = messages.subList(
                                                messages.size() - maxContextMessages, 
                                                messages.size()
                                        );
                                    }
                                    
                                    // 3. 转换为 Spring AI Message 对象
                                    for (ConversationMessage msg : contextMessages) {
                                        if (MessageType.USER.getValue().equals(msg.role())) {
                                            history.add(new UserMessage(msg.content()));
                                        } else if (MessageType.ASSISTANT.getValue().equals(msg.role())) {
                                            history.add(new AssistantMessage(msg.content()));
                                        }
                                        // 系统消息已在开头添加，数据库中的系统消息忽略
                                    }
                                    
                                    return history;
                                })
                );
    }


    /**
     * 构建动态角色消息 - 支持角色选择
     * 
     * 根据角色ID过滤配置，如果指定了角色ID，只使用该角色的配置
     * 如果没有指定角色ID，使用默认助手角色配置
     * 
     * @param roleId 选择的角色ID（可选）
     * @return 动态生成的消息列表
     */
    private Mono<List<Message>> buildDynamicRoleMessages(Long roleId) {
        Flux<RoleConfig> roleConfigsFlux;
        
        if (roleId != null) {
            // 如果指定了角色ID，只使用该角色的配置
            roleConfigsFlux = roleConfigService.getRoleConfigById(roleId)
                    .filter(RoleConfig::enabled)
                    .flux();
        } else {
            // 如果没有指定角色ID，使用默认助手角色配置
            roleConfigsFlux = roleConfigService.getRoleConfigByRoleName("默认助手")
                    .filter(RoleConfig::enabled)
                    .flux()
                    .switchIfEmpty(roleConfigService.getAllEnabledRoleConfigs());
        }
        
        return roleConfigsFlux
                .collectList()
                .flatMap(roleConfigs -> {
                    List<Message> messages = new ArrayList<>();
                    
                    return Flux.fromIterable(roleConfigs)
                            .flatMap(roleConfig -> {
                                // 为每个角色创建独立的模板变量，包含完整的角色信息
                                Map<String, Object> templateVariables = new HashMap<>();
                                
                                // 添加角色元数据变量
                                templateVariables.put("roleName", roleConfig.roleName());
                                templateVariables.put("roleDescription", roleConfig.description());
                                templateVariables.put("roleType", roleConfig.roleType());
                                
                                return roleConfigService.renderRoleTemplate(roleConfig, templateVariables)
                                        .map(renderedContent -> {
                                            Message message = createMessageByRoleType(
                                                    roleConfig.roleType(), 
                                                    renderedContent
                                            );
                                            return message;
                                        });
                            })
                            .collectList()
                            .map(renderedMessages -> {
                                messages.addAll(renderedMessages);
                                return messages;
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 如果没有配置，使用默认的系统消息
                    List<Message> defaultMessages = new ArrayList<>();
                    defaultMessages.add(new SystemMessage(chatProperties.getSystemPrompt()));
                    return Mono.just(defaultMessages);
                }));
    }

    /**
     * 根据角色类型创建对应的Message对象
     * 
     * @param roleType 角色类型
     * @param content 消息内容
     * @return Spring AI Message对象
     */
    private Message createMessageByRoleType(String roleType, String content) {
        if (content == null || content.trim().isEmpty()) {
            content = "";
        }
        
        switch (roleType) {
            case "system":
                return new SystemMessage(content);
            case "user":
                return new UserMessage(content);
            case "assistant":
                return new AssistantMessage(content);
            case "tool":
                // TOOL角色需要特殊处理，这里先返回UserMessage
                // 实际应用中可能需要创建专门的ToolMessage类
                return new UserMessage(content);
            default:
                return new SystemMessage(content);
        }
    }

    /**
     * 保存消息到数据库
     * 
     * @param sessionId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @return 操作完成信号
     */
    private Mono<Void> saveMessage(Long sessionId, String role, String content) {
        return messageRepository.save(ConversationMessage.of(sessionId, role, content))
                .then();
    }

    /**
     * 从异常中提取错误信息
     * 
     * @param e TransientAiException
     * @return 提取的错误信息
     */
    private String extractErrorMessage(TransientAiException e) {
        String message = e.getMessage();
        if (message == null) {
            return "未知错误";
        }
        
        // 尝试提取中文错误消息
        java.util.regex.Pattern zhPattern = java.util.regex.Pattern.compile("\"message_zh\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher zhMatcher = zhPattern.matcher(message);
        if (zhMatcher.find()) {
            return zhMatcher.group(1);
        }
        
        // 尝试提取英文错误消息
        java.util.regex.Pattern msgPattern = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher msgMatcher = msgPattern.matcher(message);
        if (msgMatcher.find()) {
            return msgMatcher.group(1);
        }
        
        // 截断过长的消息
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }
}
