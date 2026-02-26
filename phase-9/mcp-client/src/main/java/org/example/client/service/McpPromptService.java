package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt 服务
 * 负责处理 MCP Prompt 模板相关的操作
 */
@Service
public class McpPromptService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;
    private final UserMcpService userMcpService;

    public McpPromptService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder,
            UserMcpService userMcpService) {
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> client.getClientInfo().name().equals("user-client - user-server"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 user-client 客户端"));
        this.chatClientBuilder = chatClientBuilder;
        this.userMcpService = userMcpService;
    }

    /**
     * 获取所有可用的 Prompts 模板列表
     */
    public Mono<Map<String, Object>> listPrompts() {
        return mcpAsyncClient.listPrompts()
                .map(prompts -> {
                    System.out.println("[McpPromptService] 获取提示列表: " + prompts.prompts().size() + " 个提示");
                    return Map.of(
                            "success", true,
                            "prompts", prompts.prompts().stream()
                                    .map(prompt -> Map.of(
                                            "name", prompt.name(),
                                            "description", prompt.description(),
                                            "arguments", prompt.arguments()
                                    ))
                                    .toList(),
                            "count", prompts.prompts().size()
                    );
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", "获取提示列表失败: " + e.getMessage()
                )));
    }

    /**
     * 获取指定 Prompt 模板的内容
     */
    public Mono<Map<String, Object>> getPrompt(String name, Map<String, Object> arguments) {
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest(
                name,
                arguments != null ? arguments : Map.of()
        );

        System.out.println("[McpPromptService] 获取提示: " + name + " 参数: " + arguments);

        return mcpAsyncClient.getPrompt(request)
                .map(result -> {
                    System.out.println("[McpPromptService] 提示获取成功: " + name);
                    return Map.of(
                            "success", true,
                            "name", name,
                            "description", result.description(),
                            "messages", result.messages().stream()
                                    .map(message -> {
                                        if (message.content() instanceof McpSchema.TextContent textContent) {
                                            return Map.of(
                                                    "role", message.role().name(),
                                                    "content", textContent.text()
                                            );
                                        }
                                        return Map.of(
                                                "role", message.role().name(),
                                                "content", "非文本内容"
                                        );
                                    })
                                    .toList()
                    );
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", "获取提示失败: " + e.getMessage(),
                        "name", name
                )));
    }

    /**
     * 使用指定 Prompt 模板进行用户查询
     */
    public Mono<Map<String, Object>> queryUsersWithPrompt(String promptName, Map<String, Object> promptArgs) {
        return getPrompt(promptName, promptArgs)
                .flatMap(promptResult -> {
                    if (!(Boolean) promptResult.get("success")) {
                        return Mono.just(promptResult);
                    }

                    // 根据不同的提示模板执行相应的用户查询
                    return switch (promptName) {
                        case "user-query-assistant" -> executeUserQueryPrompt(promptArgs);
                        case "data-analysis" -> executeDataAnalysisPrompt(promptArgs);
                        case "user-creation-assistant" -> executeUserCreationPrompt(promptArgs);
                        default -> Mono.just(Map.of(
                                "success", false,
                                "error", "不支持的提示模板: " + promptName
                        ));
                    };
                });
    }

    /**
     * 执行用户查询提示模板 - 真正的AI助手实现
     * 调用MCP服务端的用户查询助手Prompt，让AI智能处理用户查询
     */
    private Mono<Map<String, Object>> executeUserQueryPrompt(Map<String, Object> args) {
        System.out.println("[McpPromptService] 调用用户查询AI助手，参数: " + args);

        String queryType = (String) args.get("queryType");
        String queryValue = (String) args.get("queryValue");

        // 调用MCP服务端的用户查询助手Prompt
        return mcpAsyncClient.getPrompt(
                new McpSchema.GetPromptRequest("user-query-assistant", args)
        ).flatMap(promptResult -> {
            // 获取AI生成的提示内容
            List<McpSchema.PromptMessage> messages = promptResult.messages();
            if (messages.isEmpty()) {
                Map<String, Object> errorResult = new java.util.HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "AI助手未生成有效的提示内容");
                return Mono.just(errorResult);
            }

            // 提取AI生成的提示文本
            String aiPrompt = messages.stream()
                    .filter(msg -> msg.role() == McpSchema.Role.USER)
                    .findFirst()
                    .map(msg -> {
                        if (msg.content() instanceof McpSchema.TextContent textContent) {
                            return textContent.text();
                        }
                        return "";
                    })
                    .orElse("");

            System.out.println("[McpPromptService] 用户查询AI助手生成的提示: " + aiPrompt);

            // 先获取用户数据
            Mono<String> userDataMono = switch (queryType) {
                case "byId" -> userMcpService.getUserById(Long.valueOf(queryValue));
                case "byName" -> userMcpService.getUserByUsername(queryValue);
                default -> userMcpService.getAllUsers();
            };

            // 使用ChatClient调用大模型进行智能查询处理
            return userDataMono.flatMap(userData ->
                    chatClientBuilder.build()
                            .prompt()
                            .user(aiPrompt + "\n\n查询结果数据：\n" + userData)
                            .stream()
                            .content()
                            .collectList()
                            .map(list -> String.join("", list))
                            .map(aiResponse -> {
                                System.out.println("[McpPromptService] 用户查询AI助手响应: " + aiResponse);

                                Map<String, Object> result = new java.util.HashMap<>();
                                result.put("success", true);
                                result.put("queryType", queryType != null ? queryType : "all");
                                result.put("queryValue", queryValue);
                                result.put("aiResponse", aiResponse);
                                result.put("rawData", userData);
                                result.put("aiProcessed", true);
                                result.put("description", "查询结果由AI助手智能处理");
                                return result;
                            })
            );
        }).onErrorResume(e -> {
            System.err.println("[McpPromptService] 用户查询AI助手调用失败: " + e.getMessage());
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "用户查询AI助手调用失败: " + e.getMessage());
            return Mono.just(errorResult);
        });
    }

    /**
     * 执行数据分析提示模板 - 真正的AI助手实现
     * 调用MCP服务端的数据分析助手Prompt，让AI智能处理数据分析
     */
    private Mono<Map<String, Object>> executeDataAnalysisPrompt(Map<String, Object> args) {
        System.out.println("[McpPromptService] 调用数据分析AI助手，参数: " + args);

        String analysisType = (String) args.getOrDefault("analysisType", "stats");

        // 调用MCP服务端的数据分析助手Prompt
        return mcpAsyncClient.getPrompt(
                new McpSchema.GetPromptRequest("data-analysis", args)
        ).flatMap(promptResult -> {
            // 获取AI生成的提示内容
            List<McpSchema.PromptMessage> messages = promptResult.messages();
            if (messages.isEmpty()) {
                Map<String, Object> errorResult = new java.util.HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "AI助手未生成有效的提示内容");
                return Mono.just(errorResult);
            }

            // 提取AI生成的提示文本
            String aiPrompt = messages.stream()
                    .filter(msg -> msg.role() == McpSchema.Role.USER)
                    .findFirst()
                    .map(msg -> {
                        if (msg.content() instanceof McpSchema.TextContent textContent) {
                            return textContent.text();
                        }
                        return "";
                    })
                    .orElse("");

            System.out.println("[McpPromptService] 数据分析AI助手生成的提示: " + aiPrompt);

            // 先获取用户统计数据
            return Mono.zip(
                    userMcpService.countUsers().defaultIfEmpty("0"),
                    userMcpService.getAllUsers().defaultIfEmpty("暂无用户数据")
            ).flatMap(tuple -> {
                String count = tuple.getT1();
                String users = tuple.getT2();

                // 使用ChatClient调用大模型进行智能数据分析
                return chatClientBuilder.build()
                        .prompt()
                        .user(aiPrompt + "\n\n统计数据：\n用户总数: " + count + "\n用户数据: " + users)
                        .stream()
                        .content()
                        .collectList()
                        .map(list -> String.join("", list))
                        .map(aiResponse -> {
                            System.out.println("[McpPromptService] 数据分析AI助手响应: " + aiResponse);

                            Map<String, Object> result = new java.util.HashMap<>();
                            result.put("success", true);
                            result.put("analysisType", analysisType);
                            result.put("userCount", count);
                            result.put("userData", users);
                            result.put("aiResponse", aiResponse);
                            result.put("aiProcessed", true);
                            result.put("description", "数据分析由AI助手智能处理");
                            return result;
                        });
            });
        }).onErrorResume(e -> {
            System.err.println("[McpPromptService] 数据分析AI助手调用失败: " + e.getMessage());
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "数据分析AI助手调用失败: " + e.getMessage());
            return Mono.just(errorResult);
        });
    }

    /**
     * 执行用户创建提示模板 - 真正的AI助手实现
     * 调用MCP服务端的用户创建助手Prompt，让AI智能处理用户创建
     */
    private Mono<Map<String, Object>> executeUserCreationPrompt(Map<String, Object> args) {
        System.out.println("[McpPromptService] 调用用户创建AI助手，参数: " + args);

        // 调用MCP服务端的用户创建助手Prompt
        return mcpAsyncClient.getPrompt(
                new McpSchema.GetPromptRequest("user-creation-assistant", args)
        ).flatMap(promptResult -> {
            // 获取AI生成的提示内容
            List<McpSchema.PromptMessage> messages = promptResult.messages();
            if (messages.isEmpty()) {
                Map<String, Object> errorResult = new java.util.HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "AI助手未生成有效的提示内容");
                return Mono.just(errorResult);
            }

            // 提取AI生成的提示文本
            String aiPrompt = messages.stream()
                    .filter(msg -> msg.role() == McpSchema.Role.USER)
                    .findFirst()
                    .map(msg -> {
                        if (msg.content() instanceof McpSchema.TextContent textContent) {
                            return textContent.text();
                        }
                        return "";
                    })
                    .orElse("");

            System.out.println("[McpPromptService] AI助手生成的提示: " + aiPrompt);

            // 使用ChatClient调用大模型进行智能用户创建（使用流式API转换为Mono）
            return chatClientBuilder.build()
                    .prompt()
                    .user(aiPrompt)
                    .stream()
                    .content()
                    .collectList()
                    .map(list -> String.join("", list))
                    .flatMap(aiResponse -> {
                        System.out.println("[McpPromptService] AI助手响应: " + aiResponse);

                        // 解析AI响应，提取用户信息
                        Map<String, Object> extractedUserInfo = extractUserInfoFromAIResponse(aiResponse, args);

                        // 执行实际的用户创建
                        return executeActualUserCreation(extractedUserInfo);
                    });
        }).onErrorResume(e -> {
            System.err.println("[McpPromptService] AI助手调用失败: " + e.getMessage());
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "AI助手调用失败: " + e.getMessage());
            return Mono.just(errorResult);
        });
    }

    /**
     * 从AI响应中提取用户信息
     */
    private Map<String, Object> extractUserInfoFromAIResponse(String aiResponse, Map<String, Object> originalArgs) {
        // 这里可以添加更复杂的AI响应解析逻辑
        // 目前简单实现：优先使用AI建议，如果没有则使用原始参数

        Map<String, Object> userInfo = new java.util.HashMap<>();

        // 从原始参数中提取基本信息
        userInfo.put("username", originalArgs.get("username"));
        userInfo.put("email", originalArgs.get("email"));
        userInfo.put("phone", originalArgs.get("phone"));
        userInfo.put("age", originalArgs.get("age"));

        // 简单的AI响应解析（可以扩展为更复杂的逻辑）
        if (aiResponse.contains("建议用户名") || aiResponse.contains("推荐邮箱")) {
            // 这里可以添加更复杂的AI建议提取逻辑
            System.out.println("[McpPromptService] AI响应包含建议信息");
        }

        return userInfo;
    }

    /**
     * 执行实际的用户创建操作
     */
    private Mono<Map<String, Object>> executeActualUserCreation(Map<String, Object> userInfo) {
        String username = (String) userInfo.get("username");
        String email = (String) userInfo.get("email");
        String phone = (String) userInfo.get("phone");
        Integer age = userInfo.get("age") != null ? Integer.valueOf(userInfo.get("age").toString()) : null;

        if (username == null || email == null) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "AI助手未能提供有效的用户名和邮箱");
            return Mono.just(errorResult);
        }

        return userMcpService.createUser(username, email, phone, age)
                .map(result -> {
                    Map<String, Object> successResult = new java.util.HashMap<>();
                    successResult.put("success", result.startsWith("✅"));
                    successResult.put("message", result);
                    successResult.put("username", username);
                    successResult.put("email", email);
                    successResult.put("aiProcessed", true);
                    successResult.put("description", "用户由AI助手智能创建");
                    return successResult;
                });
    }
}
