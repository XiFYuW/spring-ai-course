package org.example.client.handler;

import org.springaicommunity.mcp.annotation.McpPromptListChanged;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP 提示列表变更处理器
 * 处理来自 MCP 服务器的提示列表变更通知
 *
 * 使用 @McpPromptListChanged 注解声明式处理提示列表变更
 * 当服务器端的提示模板列表发生变化时（增删改提示），会触发此处理器
 */
@Component
public class McpPromptListChangedHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 存储提示列表变更历史
    private final List<PromptListChangeEvent> changeHistory = new CopyOnWriteArrayList<>();

    // 存储提示模板缓存（提示名称 -> 提示信息）
    private final Map<String, PromptInfo> promptCache = new ConcurrentHashMap<>();

    /**
     * 处理提示列表变更通知
     * 当 MCP 服务器的提示列表发生变化时触发
     */
    @McpPromptListChanged(clients = "user-server")
    public void handlePromptListChanged() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║           💬 MCP 提示列表已变更                        ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.printf("║ 时间: %s                           ║%n", timestamp);
        System.out.println("║                                                        ║");
        System.out.println("║ 服务器提示列表已更新，可能的变化：                     ║");
        System.out.println("║   • 新增提示模板                                       ║");
        System.out.println("║   • 删除提示模板                                       ║");
        System.out.println("║   • 提示内容变更                                       ║");
        System.out.println("║   • 提示参数更新                                       ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");

        // 记录变更事件
        PromptListChangeEvent event = new PromptListChangeEvent(
                System.currentTimeMillis(),
                "提示列表已更新"
        );
        changeHistory.add(event);

        // 刷新提示列表
        refreshPromptList();
    }

    /**
     * 带参数的处理方式
     * 接收变更详情
     */
    @McpPromptListChanged(clients = "user-server")
    public void handlePromptListChangedWithDetails(
            String changeType,
            List<String> affectedPrompts,
            String description) {

        String timestamp = LocalDateTime.now().format(FORMATTER);

        System.out.printf("[%s] [提示列表变更] 类型: %s%n", timestamp, changeType);

        if (description != null) {
            System.out.printf("变更描述: %s%n", description);
        }

        if (affectedPrompts != null && !affectedPrompts.isEmpty()) {
            System.out.println("受影响的提示模板:");
            affectedPrompts.forEach(prompt -> System.out.println("  - " + prompt));
        }

        // 记录详细变更事件
        PromptListChangeEvent event = new PromptListChangeEvent(
                System.currentTimeMillis(),
                changeType,
                affectedPrompts,
                description
        );
        changeHistory.add(event);

        // 更新受影响的提示缓存
        if (affectedPrompts != null) {
            affectedPrompts.forEach(this::updatePromptCache);
        }
    }

    /**
     * 刷新提示列表
     */
    private void refreshPromptList() {
        System.out.println("[提示列表变更] 正在刷新本地提示列表缓存...");
        // TODO: 调用 mcpClient.listPrompts() 刷新提示列表
        System.out.println("[提示列表变更] 提示列表刷新完成");
    }

    /**
     * 更新特定提示的缓存
     */
    private void updatePromptCache(String promptName) {
        System.out.println("[提示列表变更] 更新提示缓存: " + promptName);
        // TODO: 获取提示最新内容并更新缓存
        promptCache.put(promptName, new PromptInfo(
                promptName,
                System.currentTimeMillis()
        ));
    }

    /**
     * 获取变更历史
     */
    public List<PromptListChangeEvent> getChangeHistory() {
        return List.copyOf(changeHistory);
    }

    /**
     * 获取提示缓存
     */
    public Map<String, PromptInfo> getPromptCache() {
        return Map.copyOf(promptCache);
    }

    /**
     * 获取特定提示信息
     */
    public PromptInfo getPromptInfo(String name) {
        return promptCache.get(name);
    }

    /**
     * 检查提示是否存在
     */
    public boolean hasPrompt(String name) {
        return promptCache.containsKey(name);
    }

    /**
     * 获取所有提示名称
     */
    public List<String> getAllPromptNames() {
        return List.copyOf(promptCache.keySet());
    }

    /**
     * 清除变更历史
     */
    public void clearHistory() {
        changeHistory.clear();
    }

    /**
     * 清除提示缓存
     */
    public void clearCache() {
        promptCache.clear();
    }

    /**
     * 提示列表变更事件记录
     */
    public record PromptListChangeEvent(
            long timestamp,
            String changeType,
            List<String> affectedPrompts,
            String description) {

        public PromptListChangeEvent(long timestamp, String changeType) {
            this(timestamp, changeType, List.of(), null);
        }

        @Override
        public String toString() {
            return String.format("PromptListChangeEvent{time=%s, type='%s', prompts=%d, desc='%s'}",
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp),
                            java.time.ZoneId.systemDefault()
                    ).format(FORMATTER),
                    changeType,
                    affectedPrompts != null ? affectedPrompts.size() : 0,
                    description);
        }
    }

    /**
     * 提示信息记录
     */
    public record PromptInfo(String name, long lastUpdated, String description, List<String> arguments) {

        public PromptInfo(String name, long lastUpdated) {
            this(name, lastUpdated, null, List.of());
        }

        @Override
        public String toString() {
            return String.format("PromptInfo{name='%s', updated=%s, args=%d}",
                    name,
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastUpdated),
                            java.time.ZoneId.systemDefault()
                    ).format(FORMATTER),
                    arguments != null ? arguments.size() : 0);
        }
    }
}
