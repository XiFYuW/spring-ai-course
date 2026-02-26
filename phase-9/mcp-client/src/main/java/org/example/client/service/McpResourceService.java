package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP Resource 服务
 * 负责处理 MCP Resource 资源相关的操作
 */
@Service
public class McpResourceService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public McpResourceService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> client.getClientInfo().name().equals("user-client - user-server"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 user-client 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 获取所有可用的 Resources 列表
     */
    public Mono<Map<String, Object>> listResources() {
        return mcpAsyncClient.listResources()
                .map(resources -> {
                    System.out.println("[McpResourceService] 获取资源列表: " + resources.resources().size() + " 个资源");
                    return Map.of(
                            "success", true,
                            "resources", resources.resources().stream()
                                    .map(resource -> Map.of(
                                            "uri", resource.uri(),
                                            "name", resource.name(),
                                            "description", resource.description(),
                                            "mimeType", resource.mimeType()
                                    ))
                                    .toList(),
                            "count", resources.resources().size()
                    );
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", "获取资源列表失败: " + e.getMessage()
                )));
    }

    /**
     * 读取指定 Resource 的内容
     */
    public Mono<Map<String, Object>> readResource(String uri) {
        System.out.println("[McpResourceService] 读取资源: " + uri);

        return mcpAsyncClient.readResource(new McpSchema.ReadResourceRequest(uri))
                .map(result -> {
                    System.out.println("[McpResourceService] 资源读取成功: " + uri);
                    return Map.of(
                            "success", true,
                            "uri", uri,
                            "contents", result.contents().stream()
                                    .map(content -> {
                                        if (content instanceof McpSchema.TextResourceContents textContent) {
                                            return Map.of(
                                                    "type", "text",
                                                    "mimeType", textContent.mimeType(),
                                                    "text", textContent.text()
                                            );
                                        }
                                        return Map.of("type", "unknown");
                                    })
                                    .toList()
                    );
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", "读取资源失败: " + e.getMessage(),
                        "uri", uri
                )));
    }

    /**
     * 获取系统信息资源 - 使用AI助手智能解读
     */
    public Mono<Map<String, Object>> getSystemInfo() {
        System.out.println("[McpResourceService] 使用AI助手获取系统信息");

        return readResource("system://info")
                .flatMap(resourceResult -> {
                    if (!(Boolean) resourceResult.get("success")) {
                        return Mono.just(resourceResult);
                    }

                    // 提取资源内容
                    String resourceContent = extractResourceContent(resourceResult);

                    // 使用AI助手智能解读系统信息
                    String aiPrompt = "你是一个系统信息解读专家。请解读以下MCP服务器系统信息，" +
                            "并以友好、易懂的方式总结服务器的基本配置和能力：\n\n" + resourceContent;

                    return chatClientBuilder.build()
                            .prompt()
                            .user(aiPrompt)
                            .stream()
                            .content()
                            .collectList()
                            .map(list -> String.join("", list))
                            .map(aiResponse -> {
                                System.out.println("[McpResourceService] AI系统信息解读完成");

                                Map<String, Object> result = new java.util.HashMap<>();
                                result.put("success", true);
                                result.put("uri", "system://info");
                                result.put("rawData", resourceResult);
                                result.put("aiInterpretation", aiResponse);
                                result.put("aiProcessed", true);
                                result.put("description", "系统信息由AI助手智能解读");
                                return result;
                            });
                })
                .onErrorResume(e -> {
                    System.err.println("[McpResourceService] AI系统信息解读失败: " + e.getMessage());
                    // 如果AI处理失败，返回原始资源数据
                    return readResource("system://info");
                });
    }

    /**
     * 获取用户统计资源 - 使用AI助手智能分析
     */
    public Mono<Map<String, Object>> getUserStats() {
        System.out.println("[McpResourceService] 使用AI助手分析用户统计");

        return readResource("users://stats")
                .flatMap(resourceResult -> {
                    if (!(Boolean) resourceResult.get("success")) {
                        return Mono.just(resourceResult);
                    }

                    // 提取资源内容
                    String resourceContent = extractResourceContent(resourceResult);

                    // 使用AI助手智能分析用户统计数据
                    String aiPrompt = "你是一个数据分析专家。请分析以下用户统计数据，" +
                            "提供洞察和建议，包括用户活跃度分析、增长趋势等：\n\n" + resourceContent;

                    return chatClientBuilder.build()
                            .prompt()
                            .user(aiPrompt)
                            .stream()
                            .content()
                            .collectList()
                            .map(list -> String.join("", list))
                            .map(aiResponse -> {
                                System.out.println("[McpResourceService] AI用户统计分析完成");

                                Map<String, Object> result = new java.util.HashMap<>();
                                result.put("success", true);
                                result.put("uri", "users://stats");
                                result.put("rawData", resourceResult);
                                result.put("aiAnalysis", aiResponse);
                                result.put("aiProcessed", true);
                                result.put("description", "用户统计由AI助手智能分析");
                                return result;
                            });
                })
                .onErrorResume(e -> {
                    System.err.println("[McpResourceService] AI用户统计分析失败: " + e.getMessage());
                    // 如果AI处理失败，返回原始资源数据
                    return readResource("users://stats");
                });
    }

    /**
     * 获取服务器状态资源 - 使用AI助手智能监控
     */
    public Mono<Map<String, Object>> getServerStatus() {
        System.out.println("[McpResourceService] 使用AI助手监控服务器状态");

        return readResource("system://status")
                .flatMap(resourceResult -> {
                    if (!(Boolean) resourceResult.get("success")) {
                        return Mono.just(resourceResult);
                    }

                    // 提取资源内容
                    String resourceContent = extractResourceContent(resourceResult);

                    // 使用AI助手智能监控服务器状态
                    String aiPrompt = "你是一个系统运维专家。请分析以下服务器状态信息，" +
                            "评估系统健康状况，并提供优化建议：\n\n" + resourceContent;

                    return chatClientBuilder.build()
                            .prompt()
                            .user(aiPrompt)
                            .stream()
                            .content()
                            .collectList()
                            .map(list -> String.join("", list))
                            .map(aiResponse -> {
                                System.out.println("[McpResourceService] AI服务器状态监控完成");

                                Map<String, Object> result = new java.util.HashMap<>();
                                result.put("success", true);
                                result.put("uri", "system://status");
                                result.put("rawData", resourceResult);
                                result.put("aiMonitoring", aiResponse);
                                result.put("aiProcessed", true);
                                result.put("description", "服务器状态由AI助手智能监控");
                                return result;
                            });
                })
                .onErrorResume(e -> {
                    System.err.println("[McpResourceService] AI服务器状态监控失败: " + e.getMessage());
                    // 如果AI处理失败，返回原始资源数据
                    return readResource("system://status");
                });
    }

    /**
     * 从资源结果中提取文本内容
     */
    private String extractResourceContent(Map<String, Object> resourceResult) {
        Object contents = resourceResult.get("contents");
        if (contents instanceof List<?> contentList && !contentList.isEmpty()) {
            Object firstContent = contentList.get(0);
            if (firstContent instanceof Map<?, ?> contentMap) {
                Object text = contentMap.get("text");
                return text != null ? text.toString() : "";
            }
        }
        return "";
    }
}
