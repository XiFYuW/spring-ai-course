package org.example.client.handler;

import org.springaicommunity.mcp.annotation.McpResourceListChanged;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP 资源列表变更处理器
 * 处理来自 MCP 服务器的资源列表变更通知
 *
 * 使用 @McpResourceListChanged 注解声明式处理资源列表变更
 * 当服务器端的资源列表发生变化时（增删改资源），会触发此处理器
 */
@Component
public class McpResourceListChangedHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 存储资源列表变更历史
    private final List<ResourceListChangeEvent> changeHistory = new CopyOnWriteArrayList<>();

    // 存储当前资源列表缓存（URI -> 资源信息）
    private final Map<String, ResourceInfo> resourceCache = new ConcurrentHashMap<>();

    /**
     * 处理资源列表变更通知
     * 当 MCP 服务器的资源列表发生变化时触发
     */
    @McpResourceListChanged(clients = "user-server")
    public void handleResourceListChanged() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║           📁 MCP 资源列表已变更                        ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.printf("║ 时间: %s                           ║%n", timestamp);
        System.out.println("║                                                        ║");
        System.out.println("║ 服务器资源列表已更新，可能的变化：                     ║");
        System.out.println("║   • 新增资源                                           ║");
        System.out.println("║   • 删除资源                                           ║");
        System.out.println("║   • 资源内容变更                                       ║");
        System.out.println("║   • 资源元数据更新                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");

        // 记录变更事件
        ResourceListChangeEvent event = new ResourceListChangeEvent(
                System.currentTimeMillis(),
                "资源列表已更新"
        );
        changeHistory.add(event);

        // 刷新资源列表
        refreshResourceList();
    }

    /**
     * 带参数的处理方式
     * 接收变更详情
     */
    @McpResourceListChanged(clients = "user-server")
    public void handleResourceListChangedWithDetails(
            String changeType,
            List<String> affectedResources,
            String reason) {

        String timestamp = LocalDateTime.now().format(FORMATTER);

        System.out.printf("[%s] [资源列表变更] 类型: %s%n", timestamp, changeType);

        if (reason != null) {
            System.out.printf("变更原因: %s%n", reason);
        }

        if (affectedResources != null && !affectedResources.isEmpty()) {
            System.out.println("受影响的资源:");
            affectedResources.forEach(resource -> System.out.println("  - " + resource));
        }

        // 记录详细变更事件
        ResourceListChangeEvent event = new ResourceListChangeEvent(
                System.currentTimeMillis(),
                changeType,
                affectedResources,
                reason
        );
        changeHistory.add(event);

        // 更新受影响的资源缓存
        if (affectedResources != null) {
            affectedResources.forEach(this::updateResourceCache);
        }
    }

    /**
     * 刷新资源列表
     */
    private void refreshResourceList() {
        System.out.println("[资源列表变更] 正在刷新本地资源列表缓存...");
        // TODO: 调用 mcpClient.listResources() 刷新资源列表
        System.out.println("[资源列表变更] 资源列表刷新完成");
    }

    /**
     * 更新特定资源的缓存
     */
    private void updateResourceCache(String resourceUri) {
        System.out.println("[资源列表变更] 更新资源缓存: " + resourceUri);
        // TODO: 获取资源最新内容并更新缓存
        resourceCache.put(resourceUri, new ResourceInfo(
                resourceUri,
                System.currentTimeMillis()
        ));
    }

    /**
     * 获取变更历史
     */
    public List<ResourceListChangeEvent> getChangeHistory() {
        return List.copyOf(changeHistory);
    }

    /**
     * 获取资源缓存
     */
    public Map<String, ResourceInfo> getResourceCache() {
        return Map.copyOf(resourceCache);
    }

    /**
     * 获取特定资源信息
     */
    public ResourceInfo getResourceInfo(String uri) {
        return resourceCache.get(uri);
    }

    /**
     * 清除变更历史
     */
    public void clearHistory() {
        changeHistory.clear();
    }

    /**
     * 清除资源缓存
     */
    public void clearCache() {
        resourceCache.clear();
    }

    /**
     * 资源列表变更事件记录
     */
    public record ResourceListChangeEvent(
            long timestamp,
            String changeType,
            List<String> affectedResources,
            String reason) {

        public ResourceListChangeEvent(long timestamp, String changeType) {
            this(timestamp, changeType, List.of(), null);
        }

        @Override
        public String toString() {
            return String.format("ResourceListChangeEvent{time=%s, type='%s', resources=%d, reason='%s'}",
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp),
                            java.time.ZoneId.systemDefault()
                    ).format(FORMATTER),
                    changeType,
                    affectedResources != null ? affectedResources.size() : 0,
                    reason);
        }
    }

    /**
     * 资源信息记录
     */
    public record ResourceInfo(String uri, long lastUpdated) {
        @Override
        public String toString() {
            return String.format("ResourceInfo{uri='%s', updated=%s}",
                    uri,
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastUpdated),
                            java.time.ZoneId.systemDefault()
                    ).format(FORMATTER));
        }
    }
}
