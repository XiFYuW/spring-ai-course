package org.example.client.controller;

import org.example.client.service.McpResourceService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP Resource 资源控制器
 * 提供 Resource 资源相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/mcp/resources")
public class McpResourceController {

    private final McpResourceService mcpResourceService;

    public McpResourceController(McpResourceService mcpResourceService) {
        this.mcpResourceService = mcpResourceService;
    }

    /**
     * 获取所有可用的 Resources 列表
     * GET /api/mcp/resources
     */
    @GetMapping
    public Mono<Map<String, Object>> listResources() {
        return mcpResourceService.listResources();
    }

    /**
     * 读取指定 Resource 的内容
     * GET /api/mcp/resources/{uri}
     */
    @GetMapping("/{uri}")
    public Mono<Map<String, Object>> readResource(@PathVariable String uri) {
        return mcpResourceService.readResource(uri);
    }

    /**
     * 获取系统信息资源
     * GET /api/mcp/resources/system-info
     */
    @GetMapping("/system-info")
    public Mono<Map<String, Object>> getSystemInfo() {
        return mcpResourceService.getSystemInfo();
    }

    /**
     * 获取用户统计资源
     * GET /api/mcp/resources/user-stats
     */
    @GetMapping("/user-stats")
    public Mono<Map<String, Object>> getUserStats() {
        return mcpResourceService.getUserStats();
    }

    /**
     * 获取服务器状态资源
     * GET /api/mcp/resources/server-status
     */
    @GetMapping("/server-status")
    public Mono<Map<String, Object>> getServerStatus() {
        return mcpResourceService.getServerStatus();
    }

}
