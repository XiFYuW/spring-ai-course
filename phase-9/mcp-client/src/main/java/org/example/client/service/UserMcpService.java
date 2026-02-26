package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端服务 - 用户数据库操作服务调用者
 * 连接 MCP 服务端，调用用户增删改查工具（异步版本）
 */
@Service
public class UserMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public UserMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> {
                    return client.getClientInfo().name().equals("user-client - user-server");
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 user-client 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        if (mcpAsyncClient == null) {
            System.err.println("MCP 客户端未初始化，请检查配置");
            return;
        }
        mcpAsyncClient.listTools()
                .doOnNext(tools -> {
                    System.out.println("MCP 客户端已连接，可用工具：" + tools.tools().stream()
                            .map(McpSchema.Tool::name)
                            .toList());
                })
                .doOnError(e -> {
                    System.err.println("连接 MCP 服务器失败: " + e.getMessage());
                    System.err.println("请确保 MCP 服务器已启动 (http://localhost:8080)");
                })
                .subscribe();
    }

    /**
     * 创建用户
     */
    public Mono<String> createUser(String username, String email, String phone, Integer age) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("username", username);
        params.put("email", email);
        if (phone != null) params.put("phone", phone);
        if (age != null) params.put("age", age);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("createUser", params)
        ).map(this::extractResult);
    }

    /**
     * 根据ID查询用户
     */
    public Mono<String> getUserById(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getUserById",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * 根据用户名查询用户
     */
    public Mono<String> getUserByUsername(String username) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getUserByUsername",
                        Map.of("username", username)
                )
        ).map(this::extractResult);
    }

    /**
     * 查询所有用户
     */
    public Mono<String> getAllUsers() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getAllUsers",
                        Map.of()
                )
        ).map(this::extractResult);
    }

    /**
     * 根据状态查询用户
     */
    public Mono<String> getUsersByStatus(String status) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getUsersByStatus",
                        Map.of("status", status)
                )
        ).map(this::extractResult);
    }

    /**
     * 更新用户
     */
    public Mono<String> updateUser(Long id, String username, String email, String phone, Integer age, String status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        if (username != null) params.put("username", username);
        if (email != null) params.put("email", email);
        if (phone != null) params.put("phone", phone);
        if (age != null) params.put("age", age);
        if (status != null) params.put("status", status);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("updateUser", params)
        ).map(this::extractResult);
    }

    /**
     * 删除用户
     */
    public Mono<String> deleteUser(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "deleteUser",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * 根据年龄范围查询用户
     */
    public Mono<String> getUsersByAgeRange(Integer minAge, Integer maxAge) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getUsersByAgeRange",
                        Map.of("minAge", minAge, "maxAge", maxAge)
                )
        ).map(this::extractResult);
    }

    /**
     * 搜索用户
     */
    public Mono<String> searchUsers(String keyword) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "searchUsers",
                        Map.of("keyword", keyword)
                )
        ).map(this::extractResult);
    }

    /**
     * 统计用户总数
     */
    public Mono<String> countUsers() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "countUsers",
                        Map.of()
                )
        ).map(this::extractResult);
    }

    /**
     * 使用 AI 智能查询用户信息（流式输出）
     */
    public Flux<String> askUserAIStream(String question) {
        return Mono.zip(
                getAllUsers().defaultIfEmpty("暂无用户数据"),
                countUsers().defaultIfEmpty("0")
        ).flatMapMany(tuple -> {
            String users = tuple.getT1();
            String count = tuple.getT2();

            String prompt = String.format(
                    "你是一个用户管理系统助手。基于以下用户数据，回答用户的问题。\n\n" +
                            "用户统计：%s\n\n用户列表：\n%s\n\n用户问题：%s",
                    count, users, question
            );

            return chatClientBuilder.build()
                    .prompt(prompt)
                    .stream()
                    .content();
        }).onErrorResume(e -> Flux.just("获取用户信息失败: " + e.getMessage()));
    }

    private String extractResult(McpSchema.CallToolResult result) {
        if (result.isError()) {
            return "调用出错：" + result.content();
        }
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("无结果");
    }
}
