package org.example.client.controller;

import org.example.client.service.UserMcpService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 用户管理控制器
 * 提供 REST API 接口操作用户数据（异步版本）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMcpService userMcpService;

    public UserController(UserMcpService userMcpService) {
        this.userMcpService = userMcpService;
    }

    /**
     * 创建用户
     * POST /api/users
     */
    @PostMapping
    public Mono<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        Integer age = request.get("age") != null ? Integer.valueOf(request.get("age").toString()) : null;

        return userMcpService.createUser(username, email, phone, age)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 根据ID查询用户
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getUserById(@PathVariable Long id) {
        return userMcpService.getUserById(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "data", result
                ));
    }

    /**
     * 根据用户名查询用户
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public Mono<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        return userMcpService.getUserByUsername(username)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "data", result
                ));
    }

    /**
     * 查询所有用户
     * GET /api/users
     */
    @GetMapping
    public Mono<Map<String, Object>> getAllUsers() {
        return userMcpService.getAllUsers()
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "data", result
                ));
    }

    /**
     * 根据状态查询用户
     * GET /api/users/status/{status}
     */
    @GetMapping("/status/{status}")
    public Mono<Map<String, Object>> getUsersByStatus(@PathVariable String status) {
        return userMcpService.getUsersByStatus(status)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "status", status,
                        "data", result
                ));
    }

    /**
     * 更新用户
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        Integer age = request.get("age") != null ? Integer.valueOf(request.get("age").toString()) : null;
        String status = (String) request.get("status");

        return userMcpService.updateUser(id, username, email, phone, age, status)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 删除用户
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteUser(@PathVariable Long id) {
        return userMcpService.deleteUser(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 根据年龄范围查询用户
     * GET /api/users/age-range?minAge=20&maxAge=30
     */
    @GetMapping("/age-range")
    public Mono<Map<String, Object>> getUsersByAgeRange(
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge) {
        return userMcpService.getUsersByAgeRange(minAge, maxAge)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "minAge", minAge,
                        "maxAge", maxAge,
                        "data", result
                ));
    }

    /**
     * 搜索用户
     * GET /api/users/search?keyword=zhang
     */
    @GetMapping("/search")
    public Mono<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        return userMcpService.searchUsers(keyword)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "keyword", keyword,
                        "data", result
                ));
    }

    /**
     * 统计用户总数
     * GET /api/users/count
     */
    @GetMapping("/count")
    public Mono<Map<String, Object>> countUsers() {
        return userMcpService.countUsers()
                .map(result -> Map.of(
                        "success", true,
                        "data", result
                ));
    }

    /**
     * AI 智能用户问答（流式输出）
     * POST /api/users/ask/stream
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askUserAIStream(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "请介绍一下当前用户情况");
        return userMcpService.askUserAIStream(question);
    }

}
