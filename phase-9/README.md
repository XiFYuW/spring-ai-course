# Spring AI MCP 无状态服务器实战：构建AI智能用户管理系统

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-9](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-9)

## 目录

- [引言](#引言)
- [一、MCP 核心概念解析](#一mcp-核心概念解析)
- [二、项目架构与功能概览](#二项目架构与功能概览)
- [三、环境准备](#三环境准备)
- [四、服务端实现详解](#四服务端实现详解)
- [五、客户端实现详解](#五客户端实现详解)
- [六、运行与测试](#六运行与测试)
- [七、总结与扩展](#七总结与扩展)

---

## 引言

**Model Context Protocol (MCP)** 是 Anthropic 推出的开放协议，旨在标准化 AI 模型与外部工具、数据源的连接方式。通过 MCP，AI 助手可以安全地访问文件系统、数据库、API 等资源，实现真正的"工具调用"能力。

本文将带你从零开始，使用 **Spring AI MCP** 框架构建一个完整的**无状态用户管理系统**。你将学会：

✅ 如何配置 MCP 无状态服务器（STATELESS 模式）  
✅ 如何使用代码方式注册 Tools、Resources、Prompts  
✅ 如何实现响应式数据访问（R2DBC + PostgreSQL）  
✅ 如何构建 MCP 客户端调用远程工具  

---

## 一、MCP 核心概念解析

### 1.1 什么是 MCP？

MCP 采用**客户端-服务器架构**：

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   AI 应用       │◄───────►│  MCP 客户端      │◄───────►│  MCP 服务器      │
│  (Claude/Cursor)│         │  (Client)       │  HTTP   │  (Server)       │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                                                  │
                                                                  ▼
                                                          ┌─────────────────┐
                                                          │  工具/资源/提示  │
                                                          │  (Tools/        │
                                                          │  Resources/     │
                                                          │  Prompts)       │
                                                          └─────────────────┘
```

### 1.2 无状态服务器（STATELESS）的优势

| 特性 | 有状态服务器 | 无状态服务器 |
|------|-------------|-------------|
| 会话管理 | 维护会话状态 | 无会话状态 |
| 扩展性 | 受限于单节点 | 水平扩展友好 |
| 部署复杂度 | 需要会话同步 | 简化部署 |
| 适用场景 | 实时推送场景 | 微服务、云原生 |

**配置方式：**
```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STATELESS  # 启用无状态模式
```

### 1.3 MCP 三大核心能力

1. **Tools（工具）**：AI 可调用的函数，如查询用户、创建订单
2. **Resources（资源）**：只读数据源，如系统信息、统计数据
3. **Prompts（提示模板）**：预定义的提示词模板，如用户查询助手

---

## 二、项目架构与功能概览

### 2.1 项目结构

```
spring-ai-mcp-demo/
├── pom.xml                      # 父项目 POM
├── mcp-server/                  # MCP 服务器模块
│   ├── src/main/java/
│   │   └── org/example/server/
│   │       ├── McpServerApplication.java
│   │       ├── config/
│   │       │   ├── McpServerFeaturesConfig.java  # Tools/Resources/Prompts 配置
│   │       │   └── UserToolMethods.java          # 工具方法实现
│   │       ├── entity/
│   │       │   └── User.java                     # 用户实体
│   │       └── repository/
│   │           └── UserRepository.java           # R2DBC 数据访问
│   └── src/main/resources/
│       ├── application.yml      # 服务器配置
│       └── schema.sql           # 数据库初始化脚本
└── mcp-client/                  # MCP 客户端模块
    ├── src/main/java/
    │   └── org/example/client/
    │       ├── McpClientApplication.java
    │       ├── service/
    │       │   └── UserMcpService.java           # MCP 服务调用
    │       └── controller/
    │           └── UserController.java           # REST API
    └── src/main/resources/
        └── application.yml      # 客户端配置
```

### 2.2 功能特性

**服务端提供的 Tools：**
- `createUser` - 创建新用户
- `getUserById` - 根据 ID 查询用户
- `getUserByUsername` - 根据用户名查询
- `getAllUsers` - 查询所有用户
- `getUsersByStatus` - 按状态筛选用户
- `updateUser` - 更新用户信息
- `deleteUser` - 删除用户
- `getUsersByAgeRange` - 按年龄范围查询
- `searchUsers` - 模糊搜索用户
- `countUsers` - 统计用户总数

**服务端提供的 Resources：**
- `system://info` - 系统信息
- `users://stats` - 用户统计数据
- `system://status` - 服务器状态

**服务端提供的 Prompts：**
- `user-query-assistant` - 用户查询助手
- `data-analysis` - 数据分析助手
- `user-creation-assistant` - 用户创建助手

---

## 三、环境准备

### 3.1 技术栈要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 25+ | 使用最新特性 |
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.1.0-SNAPSHOT | AI 框架 |
| PostgreSQL | 14+ | 数据库 |
| Maven | 3.8+ | 构建工具 |

### 3.3 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3.4 创建数据库

```sql
-- 创建数据库
CREATE DATABASE chatdb;

-- 创建用户（可选）
CREATE USER mcpuser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE chatdb TO mcpuser;
```

### 3.5 父项目 POM 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>spring-ai-mcp-demo</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <spring-boot.version>3.5.10</spring-boot.version>
        <spring-ai.version>1.1.0-SNAPSHOT</spring-ai.version>
    </properties>

    <modules>
        <module>mcp-server</module>
        <module>mcp-client</module>
    </modules>

    <!-- Spring 里程碑仓库 -->
    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </repository>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>https://repo.spring.io/snapshot</url>
        </repository>
    </repositories>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## 四、服务端实现详解

### 4.1 添加依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- MCP 服务器 WebFlux 启动器 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
    </dependency>

    <!-- R2DBC 响应式数据库 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    
    <!-- PostgreSQL R2DBC 驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
</dependencies>
```

### 4.2 配置文件

```yaml
spring:
  application:
    name: mcp-user-server
  ai:
    mcp:
      server:
        protocol: STATELESS      # 关键：启用无状态模式
        enabled: true
        name: user-server
        version: 1.0.0
        type: ASYNC              # 异步模式
        capabilities:
          tool: true
          resource: true
          prompt: true
        streamable-http:
          mcp-endpoint: /api/mcp # MCP 端点路径

  # R2DBC PostgreSQL 配置
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/chatdb
    username: postgres
    password: root
    pool:
      enabled: true
      initial-size: 5
      max-size: 20

server:
  port: 8080
```

### 4.3 用户实体类

```java
package org.example.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("users")
public class User {
    @Id
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer age;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String email, String phone, Integer age) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    // Getters and Setters...
}
```

### 4.4 R2DBC 仓库接口

```java
package org.example.server.repository;

import org.example.server.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByUsername(String username);

    Mono<User> findByEmail(String email);

    Flux<User> findByStatus(String status);

    @Query("SELECT * FROM users WHERE age >= :minAge AND age <= :maxAge")
    Flux<User> findByAgeRange(Integer minAge, Integer maxAge);

    @Query("SELECT * FROM users WHERE username LIKE '%' || :keyword || '%'")
    Flux<User> findByUsernameContaining(String keyword);

    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();
}
```

### 4.5 工具方法实现

使用 `@Tool` 注解标记方法，Spring AI 会自动将其注册为 MCP 工具：

```java
package org.example.server.config;

import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import reactor.core.publisher.Mono;

public class UserToolMethods {

    private final UserRepository userRepository;

    public UserToolMethods(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Tool(name = "createUser", description = "创建新用户")
    public Mono<String> createUser(
            @ToolParam(description = "用户名，必填", required = true) String username,
            @ToolParam(description = "邮箱地址，必填", required = true) String email,
            @ToolParam(description = "手机号", required = false) String phone,
            @ToolParam(description = "年龄", required = false) Integer age) {
        
        return userRepository.findByUsername(username)
                .flatMap(existingUser -> 
                    Mono.just("❌ 创建失败：用户名 '" + username + "' 已存在"))
                .switchIfEmpty(
                    userRepository.findByEmail(email)
                        .flatMap(existingUser -> 
                            Mono.just("❌ 创建失败：邮箱 '" + email + "' 已被使用"))
                        .switchIfEmpty(
                            Mono.defer(() -> {
                                User newUser = new User(username, email, phone, age);
                                return userRepository.save(newUser)
                                    .map(savedUser -> "✅ 用户创建成功！\n" + formatUser(savedUser));
                            })
                        )
                );
    }

    @Tool(name = "getUserById", description = "根据用户ID查询用户信息")
    public Mono<String> getUserById(
            @ToolParam(description = "用户ID，必填", required = true) Long id) {
        
        return userRepository.findById(id)
                .map(user -> "✅ 查询成功！\n" + formatUser(user))
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户");
    }

    @Tool(name = "getAllUsers", description = "查询所有用户列表")
    public Mono<String> getAllUsers() {
        return userRepository.findAll()
                .collectList()
                .flatMap(users -> {
                    if (users.isEmpty()) {
                        return Mono.just("📭 暂无用户数据");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 用户列表（共 ").append(users.size()).append(" 条）：\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    @Tool(name = "deleteUser", description = "根据用户ID删除用户")
    public Mono<String> deleteUser(
            @ToolParam(description = "用户ID，必填", required = true) Long id) {
        
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    String username = existingUser.getUsername();
                    return userRepository.deleteById(id)
                            .then(Mono.just("✅ 用户删除成功！\n已删除用户：" + username));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户，无法删除");
    }

    // 更多工具方法...

    private String formatUser(User user) {
        return String.format(
            "👤 用户ID: %d\n   用户名: %s\n   邮箱: %s\n   状态: %s",
            user.getId(), user.getUsername(), user.getEmail(), user.getStatus()
        );
    }
}
```

### 4.6 MCP 功能配置（核心）

使用代码方式配置 Tools、Resources、Prompts：

```java
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
import java.util.Map;

@Configuration
public class McpServerFeaturesConfig {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public McpServerFeaturesConfig(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== Tools 配置 ====================
    @Bean
    List<ToolCallback> userTools() {
        return List.of(ToolCallbacks.from(new UserToolMethods(userRepository)));
    }

    // ==================== Resources 配置 ====================
    @Bean
    public List<McpServerFeatures.AsyncResourceSpecification> userResources() {
        return List.of(
                systemInfoResource(),
                userStatsResource(),
                serverStatusResource()
        );
    }

    private McpServerFeatures.AsyncResourceSpecification systemInfoResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("system://info")
                .name("系统信息")
                .description("提供MCP服务器的基本信息和配置")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            try {
                Map<String, Object> systemInfo = Map.of(
                        "serverName", "MCP User Server",
                        "version", "1.0.0",
                        "protocol", "STATELESS",
                        "type", "ASYNC",
                        "capabilities", List.of("tools", "resources", "prompts")
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

    private McpServerFeatures.AsyncResourceSpecification userStatsResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("users://stats")
                .name("用户统计")
                .description("提供用户数据库的统计信息")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            return userRepository.countAll()
                    .flatMap(totalCount ->
                            userRepository.findByStatus("ACTIVE").count()
                                    .map(activeCount -> {
                                        try {
                                            Map<String, Object> stats = Map.of(
                                                    "totalUsers", totalCount,
                                                    "activeUsers", activeCount,
                                                    "inactiveUsers", totalCount - activeCount
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

    // ==================== Prompts 配置 ====================
    @Bean
    public List<McpServerFeatures.AsyncPromptSpecification> userPrompts() {
        return List.of(
                userQueryPrompt(),
                dataAnalysisPrompt(),
                userCreationPrompt()
        );
    }

    private McpServerFeatures.AsyncPromptSpecification userQueryPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "user-query-assistant", 
                "用户查询助手", 
                null
        );

        return new McpServerFeatures.AsyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            String queryType = args != null ? (String) args.get("queryType") : null;

            StringBuilder promptText = new StringBuilder();
            promptText.append("你是一个用户管理系统助手。");
            promptText.append("你的任务是帮助用户查询和管理系统中的用户信息。\n\n");

            if ("byId".equals(queryType)) {
                promptText.append("请使用 getUserById 工具查询用户信息。");
            } else {
                promptText.append("请使用 getAllUsers 工具获取所有用户列表。");
            }

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

    // 更多 Prompts...
}
```

---

## 五、客户端实现详解

### 5.1 添加依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- MCP 客户端 WebFlux 启动器 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
    </dependency>

    <!-- Spring AI OpenAI 支持 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 5.2 客户端配置

```yaml
spring:
  application:
    name: user-client
  ai:
    openai:
      api-key: your-api-key
      base-url: https://ai.32zi.com  # 可替换为你的 API 代理
      chat:
        options:
          model: claude-3-7-sonnet-latest
    
    # MCP 客户端配置
    mcp:
      client:
        name: user-client
        version: 1.0.0
        enabled: true
        type: ASYNC
        streamable-http:
          connections:
            user-server:              # 连接名称
              url: http://localhost:8080
              endpoint: /api/mcp      # 服务端 MCP 端点

server:
  port: 8081
```

### 5.3 MCP 服务调用

```java
package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@Service
public class UserMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public UserMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        // 根据客户端名称筛选
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> client.getClientInfo().name().equals("user-client - user-server"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 user-client 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        // 初始化时列出可用工具
        mcpAsyncClient.listTools()
                .doOnNext(tools -> {
                    System.out.println("MCP 客户端已连接，可用工具：" + 
                        tools.tools().stream()
                            .map(McpSchema.Tool::name)
                            .toList());
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
     * 查询所有用户
     */
    public Mono<String> getAllUsers() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("getAllUsers", Map.of())
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
     * 使用 AI 智能查询用户信息
     */
    public Mono<String> askUserAI(String question) {
        return Mono.zip(
                getAllUsers().defaultIfEmpty("暂无用户数据"),
                countUsers().defaultIfEmpty("0")
        ).flatMap(tuple -> {
            String users = tuple.getT1();
            String count = tuple.getT2();

            String prompt = String.format(
                    "你是一个用户管理系统助手。基于以下用户数据，回答用户的问题。\n\n" +
                            "用户统计：%s\n\n用户列表：\n%s\n\n用户问题：%s",
                    count, users, question
            );

            return chatClientBuilder.build()
                    .prompt(prompt)
                    .call()
                    .content();
        });
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
```

### 5.4 REST 控制器

```java
package org.example.client.controller;

import org.example.client.service.UserMcpService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMcpService userMcpService;

    public UserController(UserMcpService userMcpService) {
        this.userMcpService = userMcpService;
    }

    @GetMapping
    public Mono<String> getAllUsers() {
        return userMcpService.getAllUsers();
    }

    @GetMapping("/{id}")
    public Mono<String> getUserById(@PathVariable Long id) {
        return userMcpService.getUserById(id);
    }

    @PostMapping
    public Mono<String> createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer age) {
        return userMcpService.createUser(username, email, phone, age);
    }

    @DeleteMapping("/{id}")
    public Mono<String> deleteUser(@PathVariable Long id) {
        return userMcpService.deleteUser(id);
    }

    @GetMapping("/ask")
    public Mono<String> askAI(@RequestParam String question) {
        return userMcpService.askUserAI(question);
    }
}
```

---

## 六、运行与测试

### 6.1 启动服务端

```bash
cd mcp-server
mvn spring-boot:run
```

启动成功后，控制台输出：
```
========================================
MCP 服务器已启动！
```

### 6.2 启动客户端

```bash
cd mcp-client
mvn spring-boot:run
```

客户端连接成功后，输出：
```
MCP 客户端已连接，可用工具：[createUser, getUserById, getUserByUsername, ...]
```

### 6.3 API 测试

**查询所有用户：**
```bash
curl http://localhost:8081/api/users
```

**创建用户：**
```bash
curl -X POST "http://localhost:8081/api/users?username=test&email=test@example.com&age=25"
```

**AI 智能查询：**
```bash
curl "http://localhost:8081/api/users/ask?question=统计一下用户的年龄分布"
```

### 6.4 Prompts 模板 API 测试

**获取所有 Prompts 列表：**
```bash
curl http://localhost:8081/api/mcp/prompts
```

**获取指定 Prompt 模板：**
```bash
curl http://localhost:8081/api/mcp/prompts/user-query-assistant
```

**使用用户查询助手 Prompt：**
```bash
curl -X POST http://localhost:8081/api/mcp/prompts/user-query \
  -H "Content-Type: application/json" \
  -d '{"queryType": "byId", "queryValue": "1"}'
```

**使用数据分析助手 Prompt：**
```bash
curl -X POST http://localhost:8081/api/mcp/prompts/data-analysis \
  -H "Content-Type: application/json" \
  -d '{"analysisType": "stats"}'
```

**使用用户创建助手 Prompt：**
```bash
curl -X POST http://localhost:8081/api/mcp/prompts/user-creation \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "email": "new@example.com", "age": 25}'
```

### 6.5 Resources 资源 API 测试

**获取所有 Resources 列表：**
```bash
curl http://localhost:8081/api/mcp/resources
```

**获取系统信息（AI 智能解读）：**
```bash
curl http://localhost:8081/api/mcp/resources/system-info
```

**获取用户统计（AI 智能分析）：**
```bash
curl http://localhost:8081/api/mcp/resources/user-stats
```

**获取服务器状态（AI 智能监控）：**
```bash
curl http://localhost:8081/api/mcp/resources/server-status
```

**直接读取指定 Resource：**
```bash
curl http://localhost:8081/api/mcp/resources/system%3A%2F%2Finfo
```

[建议：此处插入 Postman 测试截图或终端输出截图]

---

## 六、Prompts 和 Resources 控制器详解

### 6.1 Prompts 控制器实现

`McpPromptController` 提供了 Prompt 模板相关的 REST API 接口，支持获取提示列表、获取指定提示内容以及使用提示模板进行智能查询。

```java
package org.example.client.controller;

import org.example.client.service.McpPromptService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/prompts")
public class McpPromptController {

    private final McpPromptService mcpPromptService;

    public McpPromptController(McpPromptService mcpPromptService) {
        this.mcpPromptService = mcpPromptService;
    }

    /**
     * 获取所有可用的 Prompts 模板列表
     */
    @GetMapping
    public Mono<Map<String, Object>> listPrompts() {
        return mcpPromptService.listPrompts();
    }

    /**
     * 获取指定 Prompt 模板的内容
     */
    @GetMapping("/{name}")
    public Mono<Map<String, Object>> getPrompt(@PathVariable String name) {
        return mcpPromptService.getPrompt(name, null);
    }

    /**
     * 使用用户查询助手 Prompt 模板
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
```

### 6.2 Prompts 服务实现

`McpPromptService` 负责处理 MCP Prompt 模板相关的操作，包括获取提示列表、获取提示内容以及结合 AI 进行智能处理。

```java
package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

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
     * 结合 AI 助手智能处理查询结果
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

    // 其他执行方法...
}
```

### 6.3 Resources 控制器实现

`McpResourceController` 提供了 Resource 资源相关的 REST API 接口，支持获取资源列表、读取资源内容以及使用 AI 智能解读资源数据。

```java
package org.example.client.controller;

import org.example.client.service.McpResourceService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/resources")
public class McpResourceController {

    private final McpResourceService mcpResourceService;

    public McpResourceController(McpResourceService mcpResourceService) {
        this.mcpResourceService = mcpResourceService;
    }

    /**
     * 获取所有可用的 Resources 列表
     */
    @GetMapping
    public Mono<Map<String, Object>> listResources() {
        return mcpResourceService.listResources();
    }

    /**
     * 读取指定 Resource 的内容
     */
    @GetMapping("/{uri}")
    public Mono<Map<String, Object>> readResource(@PathVariable String uri) {
        return mcpResourceService.readResource(uri);
    }

    /**
     * 获取系统信息资源（AI 智能解读）
     */
    @GetMapping("/system-info")
    public Mono<Map<String, Object>> getSystemInfo() {
        return mcpResourceService.getSystemInfo();
    }

    /**
     * 获取用户统计资源（AI 智能分析）
     */
    @GetMapping("/user-stats")
    public Mono<Map<String, Object>> getUserStats() {
        return mcpResourceService.getUserStats();
    }

    /**
     * 获取服务器状态资源（AI 智能监控）
     */
    @GetMapping("/server-status")
    public Mono<Map<String, Object>> getServerStatus() {
        return mcpResourceService.getServerStatus();
    }
}
```

### 6.4 Resources 服务实现

`McpResourceService` 负责处理 MCP Resource 资源相关的操作，包括获取资源列表、读取资源内容以及结合 AI 进行智能解读和分析。

```java
package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

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
     * 从资源结果中提取内容文本
     */
    private String extractResourceContent(Map<String, Object> resourceResult) {
        List<Map<String, Object>> contents = (List<Map<String, Object>>) resourceResult.get("contents");
        if (contents != null && !contents.isEmpty()) {
            return (String) contents.get(0).getOrDefault("text", "");
        }
        return "";
    }
}
```

### 6.5 设计亮点

**1. AI 增强的 Prompts 处理**
- 服务端定义 Prompt 模板，客户端获取后结合 AI 进行智能处理
- 支持动态参数注入，实现个性化的 AI 助手功能
- 通过 `ChatClient` 调用大模型，实现真正的智能交互

**2. AI 增强的 Resources 解读**
- 不仅返回原始资源数据，还提供 AI 智能解读和分析
- 系统信息解读：将技术配置转化为易懂的描述
- 用户统计分析：提供数据洞察和业务建议
- 服务器状态监控：评估健康状况并提供优化建议

**3. 错误处理与降级策略**
- AI 处理失败时自动返回原始数据，保证服务可用性
- 使用 `onErrorResume` 实现优雅的错误恢复

---

## 七、总结与扩展

### 7.1 本文核心要点

1. **无状态 MCP 服务器**适合云原生部署，通过 `protocol: STATELESS` 启用
2. **代码方式配置**比注解方式更灵活，适合复杂业务场景
3. **R2DBC + WebFlux** 提供了全响应式技术栈
4. **Tools/Resources/Prompts** 三大能力覆盖 AI 交互全场景

### 7.2 可扩展方向

1. **添加认证鉴权**：
   - 使用 Spring Security 保护 MCP 端点
   - 实现 API Key 或 JWT 验证

2. **集成更多 AI 模型**：
   - 支持 Claude、GPT-4、文心一言等
   - 实现模型路由和负载均衡

3. **添加监控和日志**：
   - 集成 Micrometer 和 Prometheus
   - 记录工具调用日志

4. **支持更多数据库**：
   - MySQL、MongoDB、Redis
   - 实现多数据源切换

### 7.3 参考资料

- [Spring AI MCP 官方文档](https://docs.springframework.org.cn/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html)
- [MCP 协议规范](https://modelcontextprotocol.io/)
- [Spring R2DBC 文档](https://docs.spring.io/spring-framework/reference/data-access/r2dbc.html)

---

## 小结

本文详细介绍了如何使用 Spring AI MCP 构建无状态用户管理系统，涵盖服务端和客户端的完整实现。通过本教程，你应该已经掌握了：

✅ MCP 协议的核心概念  
✅ Spring AI MCP 的配置方式  
✅ Tools、Resources、Prompts 的实现方法  
✅ 响应式编程在 MCP 中的应用  

---

💰 **为什么选择 32ai？**

**低至 0.56 : 1 比率**  
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

---

欢迎在评论区交流讨论！如有问题，请随时提出。

**原创声明**：本文为原创教程，转载请注明出处
