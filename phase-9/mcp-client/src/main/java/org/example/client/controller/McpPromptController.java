package org.example.client.controller;

import org.example.client.service.McpPromptService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP Prompt 模板控制器
 * 提供 Prompt 模板相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/mcp/prompts")
public class McpPromptController {

    private final McpPromptService mcpPromptService;

    public McpPromptController(McpPromptService mcpPromptService) {
        this.mcpPromptService = mcpPromptService;
    }

    /**
     * 获取所有可用的 Prompts 模板列表
     * GET /api/mcp/prompts
     */
    @GetMapping
    public Mono<Map<String, Object>> listPrompts() {
        return mcpPromptService.listPrompts();
    }

    /**
     * 获取指定 Prompt 模板的内容
     * GET /api/mcp/prompts/{name}
     */
    @GetMapping("/{name}")
    public Mono<Map<String, Object>> getPrompt(@PathVariable String name) {
        return mcpPromptService.getPrompt(name, null);
    }

    /**
     * 使用用户查询助手 Prompt 模板
     * POST /api/mcp/prompts/user-query
     */
    @PostMapping("/user-query")
    public Mono<Map<String, Object>> queryUsersWithUserQueryPrompt(
            @RequestBody Map<String, Object> request) {
        String queryType = (String) request.getOrDefault("queryType", "all");
        String queryValue = (String) request.get("queryValue");

        Map<String, Object> promptArgs = new java.util.HashMap<>();
        promptArgs.put("queryType", queryType);
        if (queryValue != null) {
            promptArgs.put("queryValue", queryValue);
        }

        return mcpPromptService.queryUsersWithPrompt("user-query-assistant", promptArgs);
    }

    /**
     * 使用数据分析助手 Prompt 模板
     * POST /api/mcp/prompts/data-analysis
     */
    @PostMapping("/data-analysis")
    public Mono<Map<String, Object>> analyzeUsersWithDataAnalysisPrompt(
            @RequestBody Map<String, Object> request) {
        String analysisType = (String) request.getOrDefault("analysisType", "stats");

        Map<String, Object> promptArgs = new java.util.HashMap<>();
        promptArgs.put("analysisType", analysisType);

        return mcpPromptService.queryUsersWithPrompt("data-analysis", promptArgs);
    }

    /**
     * 使用用户创建助手 Prompt 模板
     * POST /api/mcp/prompts/user-creation
     */
    @PostMapping("/user-creation")
    public Mono<Map<String, Object>> createUserWithUserCreationPrompt(
            @RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        Integer age = request.get("age") != null ? Integer.valueOf(request.get("age").toString()) : null;

        Map<String, Object> promptArgs = new java.util.HashMap<>();
        if (username != null) promptArgs.put("username", username);
        if (email != null) promptArgs.put("email", email);
        if (phone != null) promptArgs.put("phone", phone);
        if (age != null) promptArgs.put("age", age);

        return mcpPromptService.queryUsersWithPrompt("user-creation-assistant", promptArgs);
    }
}
