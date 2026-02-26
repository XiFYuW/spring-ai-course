package org.example.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.example.server.repository.UserRepository;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

import java.util.Arrays;
import java.util.Map;

/**
 * MCP 服务器功能配置类
 * 使用代码方式配置 Tools、Resources 和 Prompts
 * 替代原有的注解方式
 */
@Configuration
public class McpServerFeaturesConfig {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public McpServerFeaturesConfig(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== Tools 配置 ====================

    /**
     * 配置用户管理工具
     * 使用 Spring AI ToolCallback 方式
     */
    @Bean
    List<ToolCallback> userTools() {
        return Arrays.asList(ToolCallbacks.from(new UserToolMethods(userRepository)));
    }

    // ==================== Resources 配置 ====================

    /**
     * 配置系统信息资源
     * 使用 Spring AI AsyncResourceSpecification 配置方式
     * 结合客户端@McpResourceListChanged注解处理服务器资源列表更改时的通知。
     */
    @Bean
    public List<McpServerFeatures.AsyncResourceSpecification> userResources() {
        return List.of(
                // 1. 系统信息资源
                systemInfoResource(),
                // 2. 用户统计资源
                userStatsResource(),
                // 3. 服务器状态资源
                serverStatusResource()
        );
    }


    /**
     * 系统信息资源
     */
    private McpServerFeatures.AsyncResourceSpecification systemInfoResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("system://info")
                .name("系统信息")
                .description("提供MCP服务器的基本信息和配置")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            System.out.println("[MCP Resource: system://info] 读取系统信息");

            try {
                Map<String, Object> systemInfo = Map.of(
                        "serverName", "MCP User Server",
                        "version", "1.0.0",
                        "protocol", "STATELESS",
                        "type", "ASYNC",
                        "capabilities", List.of("tools", "resources", "prompts"),
                        "timestamp", java.time.Instant.now().toString()
                );

                String jsonContent = objectMapper.writeValueAsString(systemInfo);

                return Mono.just(new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "application/json",
                                jsonContent
                        ))
                ));
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Failed to generate system info", e));
            }
        });
    }

    /**
     * 用户统计资源
     */
    private McpServerFeatures.AsyncResourceSpecification userStatsResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("users://stats")
                .name("用户统计")
                .description("提供用户数据库的统计信息")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            System.out.println("[MCP Resource: users://stats] 读取用户统计");

            return userRepository.countAll()
                    .flatMap(totalCount ->
                            userRepository.findByStatus("ACTIVE").count()
                                    .map(activeCount -> {
                                        try {
                                            Map<String, Object> stats = Map.of(
                                                    "totalUsers", totalCount,
                                                    "activeUsers", activeCount,
                                                    "inactiveUsers", totalCount - activeCount,
                                                    "timestamp", java.time.Instant.now().toString()
                                            );

                                            String jsonContent = objectMapper.writeValueAsString(stats);

                                            return new McpSchema.ReadResourceResult(
                                                    List.of(new McpSchema.TextResourceContents(
                                                            request.uri(),
                                                            "application/json",
                                                            jsonContent
                                                    ))
                                            );
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to generate stats", e);
                                        }
                                    })
                    );
        });
    }

    /**
     * 服务器状态资源
     */
    private McpServerFeatures.AsyncResourceSpecification serverStatusResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("system://status")
                .name("服务器状态")
                .description("提供服务器运行状态和健康信息")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            System.out.println("[MCP Resource: system://status] 读取服务器状态");

            Runtime runtime = Runtime.getRuntime();

            try {
                Map<String, Object> status = Map.of(
                        "status", "healthy",
                        "uptime", java.time.Instant.now().toString(),
                        "memory", Map.of(
                                "total", runtime.totalMemory(),
                                "free", runtime.freeMemory(),
                                "used", runtime.totalMemory() - runtime.freeMemory()
                        ),
                        "processors", runtime.availableProcessors()
                );

                String jsonContent = objectMapper.writeValueAsString(status);

                return Mono.just(new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "application/json",
                                jsonContent
                        ))
                ));
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Failed to generate status", e));
            }
        });
    }

    // ==================== Prompts 配置 ====================

    /**
     * 配置提示模板
     * 使用 Spring AI AsyncPromptSpecification 配置方式
     * 结合客户端@McpPromptListChanged注解处理服务器提示列表更改时的通知。
     */
    @Bean
    public List<McpServerFeatures.AsyncPromptSpecification> userPrompts() {
        return List.of(
                // 1. 用户查询助手提示
                userQueryPrompt(),
                // 2. 数据分析提示
                dataAnalysisPrompt(),
                // 3. 用户创建助手提示
                userCreationPrompt()
        );
    }

    /**
     * 用户查询助手提示
     */
    private McpServerFeatures.AsyncPromptSpecification userQueryPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt("user-query-assistant", "用户查询助手", null);

        return new McpServerFeatures.AsyncPromptSpecification(prompt, (exchange, request) -> {
            System.out.println("[MCP Prompt: user-query-assistant] 生成提示");

            Map<String, Object> args = request.arguments();
            String queryType = args != null ? (String) args.get("queryType") : null;
            String queryValue = args != null ? (String) args.get("queryValue") : null;

            StringBuilder promptText = new StringBuilder();
            promptText.append("你是一个用户管理系统助手。");
            promptText.append("你的任务是帮助用户查询和管理系统中的用户信息。\n\n");

            if ("byId".equals(queryType) && queryValue != null) {
                promptText.append("当前任务：根据ID查询用户\n");
                promptText.append("用户ID：").append(queryValue).append("\n\n");
                promptText.append("请使用 getUserById 工具查询用户信息。");
            } else if ("byName".equals(queryType) && queryValue != null) {
                promptText.append("当前任务：根据用户名查询用户\n");
                promptText.append("用户名：").append(queryValue).append("\n\n");
                promptText.append("请使用 getUserByUsername 工具查询用户信息。");
            } else {
                promptText.append("当前任务：列出所有用户\n\n");
                promptText.append("请使用 getAllUsers 工具获取所有用户列表。");
            }

            promptText.append("\n\n请以友好、专业的方式展示查询结果。");

            McpSchema.PromptMessage message = new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(promptText.toString())
            );

            return Mono.just(new McpSchema.GetPromptResult(
                    "用户查询助手提示",
                    List.of(message)
            ));
        });
    }

    /**
     * 数据分析提示
     */
    private McpServerFeatures.AsyncPromptSpecification dataAnalysisPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt("data-analysis", "数据分析助手", null);

        return new McpServerFeatures.AsyncPromptSpecification(prompt, (exchange, request) -> {
            System.out.println("[MCP Prompt: data-analysis] 生成提示");

            Map<String, Object> args = request.arguments();
            String analysisType = args != null ? (String) args.get("analysisType") : "stats";

            StringBuilder promptText = new StringBuilder();
            promptText.append("你是一个数据分析专家。\n\n");

            switch (analysisType) {
                case "trends":
                    promptText.append("当前任务：分析用户增长趋势\n\n");
                    promptText.append("请分析用户的创建时间分布，识别增长趋势和模式。");
                    break;
                case "distribution":
                    promptText.append("当前任务：分析用户分布\n\n");
                    promptText.append("请分析用户的年龄分布、状态分布等统计信息。");
                    break;
                default:
                    promptText.append("当前任务：用户数据统计分析\n\n");
                    promptText.append("请先获取用户总数和活跃用户数量，然后提供综合分析报告。");
                    break;
            }

            promptText.append("\n\n请使用 countUsers 和 getAllUsers 工具获取数据。");
            promptText.append("\n以清晰、易懂的方式呈现分析结果。");

            McpSchema.PromptMessage message = new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(promptText.toString())
            );

            return Mono.just(new McpSchema.GetPromptResult(
                    "数据分析助手提示",
                    List.of(message)
            ));
        });
    }

    /**
     * 用户创建助手提示
     */
    private McpServerFeatures.AsyncPromptSpecification userCreationPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt("user-creation-assistant", "用户创建助手", null);

        return new McpServerFeatures.AsyncPromptSpecification(prompt, (exchange, request) -> {
            System.out.println("[MCP Prompt: user-creation-assistant] 生成提示");

            Map<String, Object> args = request.arguments();
            String username = args != null ? (String) args.get("username") : null;
            String email = args != null ? (String) args.get("email") : null;

            StringBuilder promptText = new StringBuilder();
            promptText.append("你是一个用户创建助手。\n\n");
            promptText.append("你的任务是帮助创建新用户账户。\n\n");

            if (username != null || email != null) {
                promptText.append("建议的用户信息：\n");
                if (username != null) {
                    promptText.append("- 用户名：").append(username).append("\n");
                }
                if (email != null) {
                    promptText.append("- 邮箱：").append(email).append("\n");
                }
                promptText.append("\n");
            }

            promptText.append("创建用户需要以下信息：\n");
            promptText.append("1. 用户名（必填）：唯一标识用户\n");
            promptText.append("2. 邮箱（必填）：用于联系和验证\n");
            promptText.append("3. 手机号（可选）：额外联系方式\n");
            promptText.append("4. 年龄（可选）：用户年龄信息\n\n");

            promptText.append("请使用 createUser 工具创建用户。");
            promptText.append("\n创建前请检查用户名和邮箱是否已存在。");

            McpSchema.PromptMessage message = new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(promptText.toString())
            );

            return Mono.just(new McpSchema.GetPromptResult(
                    "用户创建助手提示",
                    List.of(message)
            ));
        });
    }
}
