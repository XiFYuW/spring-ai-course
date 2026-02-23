# 告别 STDIO/SSE：Spring AI Streamable HTTP MCP 实战指南

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-8](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-8)

## 引言

在 AI 应用开发中，如何让大语言模型（LLM）安全、高效地访问企业数据和服务，是一个核心挑战。**Model Context Protocol (MCP)** 是 Anthropic 提出的一种开放协议，旨在标准化 AI 模型与外部工具、数据源的连接方式。

传统的 MCP 实现多采用 STDIO 或 SSE（Server-Sent Events）传输方式，但在企业级应用中，它们都存在一定局限：STDIO 难以跨网络通信，SSE 则是单向推送。而 **Streamable HTTP** —— 可流式传输的 HTTP MCP 服务器，结合了 HTTP 的普适性和流式传输的实时性，成为构建生产级 MCP 服务的理想选择。

本文将带你实战构建一个基于 **Streamable HTTP** 传输协议的 **MCP 客户端-服务器分离架构**用户管理系统：
- **MCP Server**：基于 `spring-ai-starter-mcp-server-webflux` 提供用户 CRUD 工具服务，采用 R2DBC + PostgreSQL 响应式数据库
- **MCP Client**：通过 `spring-ai-starter-mcp-client-webflux` 连接 Server，结合 OpenAI 实现智能问答

**读完本文，你将收获**：
- 深入理解 MCP 协议的核心概念与 Streamable HTTP 传输机制
- 掌握 Spring AI MCP 的注解式工具开发与 WebFlux 响应式编程
- 学会构建基于 Streamable HTTP 的分离式 AI 服务架构
- 实战完整的用户管理系统（增删改查 + AI 智能问答）

---

## 目录

- [一、MCP 协议核心概念](#一mcp-协议核心概念)
- [二、项目架构设计](#二项目架构设计)
- [三、环境准备](#三环境准备)
- [四、MCP Server 开发](#四mcp-server-开发)
- [五、MCP Client 开发](#五mcp-client-开发)
- [六、运行与测试](#六运行与测试)
- [七、避坑指南](#七避坑指南)
- [八、总结与扩展](#八总结与扩展)

---

## 一、MCP 协议核心概念

### 1.1 什么是 MCP？

**Model Context Protocol (MCP)** 是一种开放协议，它标准化了应用程序如何向 LLM 提供上下文信息。可以将其理解为 AI 应用的 **"USB-C 接口"** —— 统一、标准化、即插即用。

### 1.2 MCP 核心组件

| 组件 | 说明 |
|------|------|
| **Server** | 提供工具（Tools）、资源（Resources）、提示（Prompts）的服务端 |
| **Client** | 连接到 Server，发现并调用其提供的能力 |
| **Tool** | 可被 LLM 调用的函数/方法，如查询数据库、发送邮件 |
| **Resource** | 可被读取的数据源，如文件、数据库记录 |
| **Prompt** | 预定义的提示模板 |

### 1.3 为什么需要分离架构？

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   MCP Client    │ ──────> │   MCP Server    │ ──────> │   PostgreSQL    │
│  (AI 应用层)     │  HTTP   │  (工具服务层)    │  R2DBC  │   (数据层)       │
│                 │ <────── │                 │ <────── │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
       │                            │
       v                            v
  调用 AI 模型                   执行业务逻辑
  智能问答                       数据操作
```

**分离架构的优势**：
- **安全性**：敏感数据库不直接暴露给客户端
- **复用性**：多个客户端可共享同一套工具服务
- **可维护性**：工具逻辑与 AI 应用解耦
- **扩展性**：易于添加新的工具或客户端

---

## 二、项目架构设计

### 2.1 项目结构

```
spring-ai-mcp-demo/
├── pom.xml                    # 父 POM，统一管理依赖版本
├── mcp-server/                # MCP 服务器模块
│   ├── src/main/java/
│   │   └── org/example/server/
│   │       ├── McpServerApplication.java
│   │       ├── entity/User.java           # 用户实体
│   │       ├── repository/UserRepository.java  # R2DBC 仓库
│   │       └── tool/UserTools.java        # MCP 工具定义
│   ├── src/main/resources/
│   │   ├── application.yml    # 服务器配置
│   │   └── schema.sql         # 数据库初始化脚本
│   └── pom.xml
└── mcp-client/                # MCP 客户端模块
    ├── src/main/java/
    │   └── org/example/client/
    │       ├── McpClientApplication.java
    │       ├── controller/UserController.java  # REST API
    │       └── service/UserMcpService.java     # MCP 服务调用
    ├── src/main/resources/
    │   └── application.yml      # 客户端配置
    └── pom.xml
```

### 2.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.5.10 |
| AI 框架 | Spring AI | 1.1.0-SNAPSHOT |
| MCP 实现 | Spring AI MCP | 1.1.0-SNAPSHOT |
| 响应式编程 | Spring WebFlux | 3.5.10 |
| 数据库 | R2DBC PostgreSQL | - |
| AI 模型 | OpenAI API | - |

---

## 三、环境准备

### 3.1 前置条件

1. **JDK 25**（或兼容版本）
2. **Maven 3.8+**
3. **PostgreSQL 数据库**
4. **OpenAI API Key**（本文使用 32ai 代理服务）

### 3.2 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3.3 创建数据库

```sql
-- 创建数据库
CREATE DATABASE chatdb;

-- 创建用户（可选）
CREATE USER postgres WITH PASSWORD 'root';
GRANT ALL PRIVILEGES ON DATABASE chatdb TO postgres;
```

### 3.4 父 POM 配置

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

    <repositories>
        <!-- Spring 里程碑仓库 -->
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </repository>
        <!-- Spring 快照仓库 -->
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

## 四、MCP Server 开发

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
    <!-- PostgreSQL驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
</dependencies>
```

### 4.2 配置 application.yml

```yaml
spring:
  application:
    name: mcp-user-server
  http:
    codecs:
      max-in-memory-size: 10MB
  ai:
    mcp:
      server:
        protocol: STREAMABLE  # 可流式传输的 HTTP MCP 服务
        enabled: true
        name: webflux-mcp-server
        version: 1.0.0
        type: ASYNC
        instructions: "This reactive server provides user management tools and resources"
        annotation-scanner:
          enabled: true       # 启用注解扫描
        capabilities:
          tool: true
          resource: true
          prompt: true
        streamable-http:
          mcp-endpoint: /api/mcp
          keep-alive-interval: 30s

  # R2DBC PostgreSQL 数据库配置
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/chatdb
    username: postgres
    password: root
    pool:
      enabled: true
      initial-size: 5
      max-size: 20
  sql:
    init:
      mode: always  # 自动执行 schema.sql

server:
  port: 8080
```

### 4.3 创建用户实体

```java
package org.example.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 users
 */
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

    public User() {
    }

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

### 4.4 创建 R2DBC 仓库

```java
package org.example.server.repository;

import org.example.server.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用户数据访问层接口
 * 使用R2DBC响应式编程操作数据库
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    /**
     * 根据用户名查询用户
     */
    Mono<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    Mono<User> findByEmail(String email);

    /**
     * 根据状态查询用户列表
     */
    Flux<User> findByStatus(String status);

    /**
     * 根据年龄范围查询用户
     */
    @Query("SELECT * FROM users WHERE age >= :minAge AND age <= :maxAge")
    Flux<User> findByAgeRange(Integer minAge, Integer maxAge);

    /**
     * 模糊查询用户名
     */
    @Query("SELECT * FROM users WHERE username LIKE '%' || :keyword || '%'")
    Flux<User> findByUsernameContaining(String keyword);

    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();
}
```

### 4.5 创建 MCP 工具（核心）

```java
package org.example.server.tool;

import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.format.DateTimeFormatter;

/**
 * MCP 服务器 - 用户数据库操作工具提供者
 * 使用 Spring AI MCP 注解暴露用户增删改查功能（异步版本）
 * 基于 R2DBC + PostgreSQL 响应式数据库
 */
@Component
public class UserTools {

    private final UserRepository userRepository;

    public UserTools(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 创建新用户
     */
    @McpTool(
            name = "createUser",
            description = "创建新用户，需要用户名、邮箱、手机号和年龄"
    )
    public Mono<String> createUser(
            @McpToolParam(description = "用户名，必填", required = true) String username,
            @McpToolParam(description = "邮箱地址，必填", required = true) String email,
            @McpToolParam(description = "手机号", required = false) String phone,
            @McpToolParam(description = "年龄", required = false) Integer age) {

        System.out.println("[UserTools] 开始创建用户: " + username);

        return userRepository.findByUsername(username)
                .flatMap(existingUser -> {
                    String errorMsg = "❌ 创建失败：用户名 '" + username + "' 已存在";
                    return Mono.just(errorMsg);
                })
                .switchIfEmpty(
                        userRepository.findByEmail(email)
                                .flatMap(existingUser -> {
                                    String errorMsg = "❌ 创建失败：邮箱 '" + email + "' 已被使用";
                                    return Mono.just(errorMsg);
                                })
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            User newUser = new User(username, email, phone, age);
                                            return userRepository.save(newUser)
                                                    .map(savedUser -> {
                                                        String successMsg = "✅ 用户创建成功: " + username;
                                                        return "✅ 用户创建成功！\n" + formatUser(savedUser);
                                                    });
                                        })
                                )
                );
    }

    /**
     * 根据ID查询用户
     */
    @McpTool(
            name = "getUserById",
            description = "根据用户ID查询用户信息"
    )
    public Mono<String> getUserById(
            @McpToolParam(description = "用户ID，必填", required = true) Long id) {

        return userRepository.findById(id)
                .map(user -> "✅ 查询成功！\n" + formatUser(user))
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户");
    }

    /**
     * 查询所有用户
     */
    @McpTool(
            name = "getAllUsers",
            description = "查询所有用户列表"
    )
    public Mono<String> getAllUsers() {
        return userRepository.findAll()
                .collectList()
                .flatMap(users -> {
                    if (users.isEmpty()) {
                        return Mono.just("📭 暂无用户数据");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 用户列表（共 ").append(users.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 更新用户
     */
    @McpTool(
            name = "updateUser",
            description = "根据用户ID更新用户信息"
    )
    public Mono<String> updateUser(
            @McpToolParam(description = "用户ID，必填", required = true) Long id,
            @McpToolParam(description = "新用户名（不修改传null）", required = false) String username,
            @McpToolParam(description = "新邮箱（不修改传null）", required = false) String email,
            @McpToolParam(description = "新手机号（不修改传null）", required = false) String phone,
            @McpToolParam(description = "新年龄（不修改传null）", required = false) Integer age,
            @McpToolParam(description = "新状态：ACTIVE、INACTIVE、DISABLED", required = false) String status) {

        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    if (username != null && !username.isEmpty()) {
                        existingUser.setUsername(username);
                    }
                    if (email != null && !email.isEmpty()) {
                        existingUser.setEmail(email);
                    }
                    if (phone != null) {
                        existingUser.setPhone(phone);
                    }
                    if (age != null) {
                        existingUser.setAge(age);
                    }
                    if (status != null && !status.isEmpty()) {
                        existingUser.setStatus(status.toUpperCase());
                    }
                    existingUser.setUpdatedAt(java.time.LocalDateTime.now());

                    return userRepository.save(existingUser)
                            .map(updatedUser -> "✅ 用户更新成功！\n" + formatUser(updatedUser));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户，无法更新");
    }

    /**
     * 删除用户
     */
    @McpTool(
            name = "deleteUser",
            description = "根据用户ID删除用户"
    )
    public Mono<String> deleteUser(
            @McpToolParam(description = "用户ID，必填", required = true) Long id) {

        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    String username = existingUser.getUsername();
                    return userRepository.deleteById(id)
                            .then(Mono.just("✅ 用户删除成功！\n已删除用户：" + username));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户，无法删除");
    }

    /**
     * 统计用户总数
     */
    @McpTool(
            name = "countUsers",
            description = "统计系统中的用户总数"
    )
    public Mono<String> countUsers() {
        return userRepository.countAll()
                .map(count -> "📊 系统用户总数：" + count + " 人");
    }

    /**
     * 格式化用户对象为字符串
     */
    private String formatUser(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "👤 用户ID: %d\n" +
                "   用户名: %s\n" +
                "   邮箱: %s\n" +
                "   手机号: %s\n" +
                "   年龄: %d\n" +
                "   状态: %s\n" +
                "   创建时间: %s\n" +
                "   更新时间: %s",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone() != null ? user.getPhone() : "未设置",
                user.getAge() != null ? user.getAge() : 0,
                user.getStatus(),
                user.getCreatedAt().format(formatter),
                user.getUpdatedAt().format(formatter)
        );
    }
}
```

**核心注解说明**：
- `@McpTool`：标记方法为 MCP 工具，`name` 指定工具名，`description` 供 LLM 理解工具用途
- `@McpToolParam`：标记参数，`description` 帮助 LLM 正确传参，`required` 标记是否必填

### 4.6 数据库初始化脚本

```sql
-- 用户表初始化脚本
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    age INTEGER,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以提高查询性能
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- 插入示例数据
INSERT INTO users (username, email, phone, age, status) VALUES
('zhangsan', 'zhangsan@example.com', '13800138001', 25, 'ACTIVE'),
('lisi', 'lisi@example.com', '13800138002', 30, 'ACTIVE'),
('wangwu', 'wangwu@example.com', '13800138003', 28, 'INACTIVE');
```

---

## 五、MCP Client 开发

### 5.1 添加依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Spring Boot Web（用于 REST API） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MCP 客户端 WebFlux 启动器 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
    </dependency>

    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

### 5.2 配置 application.yml

```yaml
spring:
  application:
    name: mcp-user-client
  http:
    codecs:
      max-in-memory-size: 10MB
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://ai.32zi.com  # 使用 32ai 代理服务
      chat:
        options:
          model: claude-3-7-sonnet-latest
    retry:
      max-attempts: 5
      backoff:
        initial-interval: 2000
        multiplier: 2
    # MCP 客户端配置
    mcp:
      client:
        name: user-server
        version: 1.0.0
        enabled: true
        type: ASYNC
        request-timeout: 30s
        streamable-http:
          connections:
            user-server:
              url: http://localhost:8080      # MCP Server 地址
              endpoint: /api/mcp              # MCP 端点

server:
  port: 8081

logging:
  level:
    io.modelcontextprotocol: DEBUG
    org.springframework.ai.mcp: DEBUG
```

### 5.3 创建 MCP 服务调用层

```java
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
        // 根据客户端名称筛选特定的 MCP 客户端
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> client.getClientInfo().name().equals("user-server - user-server"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 user-server 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        // 初始化时列出可用工具
        mcpAsyncClient.listTools()
                .doOnNext(tools -> {
                    System.out.println("MCP 客户端已连接，可用工具：" + tools.tools().stream()
                            .map(McpSchema.Tool::name)
                            .toList());
                })
                .doOnError(e -> {
                    System.err.println("连接 MCP 服务器失败: " + e.getMessage());
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
                new McpSchema.CallToolRequest(
                        "getAllUsers",
                        Map.of()
                )
        ).map(this::extractResult);
    }

    /**
     * 更新用户
     */
    public Mono<String> updateUser(Long id, String username, String email, 
                                   String phone, Integer age, String status) {
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

    /**
     * 提取工具调用结果
     */
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

### 5.4 创建 REST 控制器

```java
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
        Integer age = request.get("age") != null ? 
                Integer.valueOf(request.get("age").toString()) : null;

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
        Integer age = request.get("age") != null ? 
                Integer.valueOf(request.get("age").toString()) : null;
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
```

---

## 六、运行与测试

### 6.1 启动服务

**第一步：启动 MCP Server**

```bash
cd mcp-server
mvn spring-boot:run
```

看到以下输出表示启动成功：
```
========================================
MCP 服务器已启动！
```

**第二步：启动 MCP Client**

```bash
cd mcp-client
mvn spring-boot:run
```

看到以下输出表示连接成功：
```
========================================
MCP 客户端已启动！
API 地址: http://localhost:8081
MCP 客户端已连接，可用工具：[createUser, getUserById, getAllUsers, ...]
```

### 6.2 API 测试

**创建用户**：
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "phone": "13800138000",
    "age": 25
  }'
```

**查询所有用户**：
```bash
curl http://localhost:8081/api/users
```

**根据ID查询**：
```bash
curl http://localhost:8081/api/users/1
```

**更新用户**：
```bash
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13900139000",
    "age": 26
  }'
```

**删除用户**：
```bash
curl -X DELETE http://localhost:8081/api/users/1
```

**AI 智能问答（流式）**：
```bash
curl -X POST http://localhost:8081/api/users/ask/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "统计一下用户情况，并给出分析建议"
  }'
```

[建议：此处插入 Postman 测试截图或终端输出截图]

---

## 七、避坑指南

### 7.1 依赖版本冲突

**问题**：Spring AI MCP 1.1.0-SNAPSHOT 与 Spring Boot 3.5.x 可能存在兼容性问题。

**解决**：统一使用父 POM 管理版本，确保所有模块版本一致。

### 7.2 MCP 客户端连接失败

**问题**：客户端启动时报错 "未找到 user-server 客户端"。

**解决**：
1. 检查 `application.yml` 中的 `spring.ai.mcp.client.name` 是否与 Server 端配置一致
2. 确保 Server 先于 Client 启动
3. 检查 Server 端口是否被占用

### 7.3 R2DBC 连接池问题

**问题**：数据库连接超时或耗尽。

**解决**：
```yaml
spring:
  r2dbc:
    pool:
      enabled: true
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
```

### 7.4 异步编程注意事项

**问题**：Mono/Flux 链式调用中出现阻塞操作。

**解决**：
- 避免在响应式链中使用阻塞 IO
- 使用 `Mono.defer()` 延迟执行
- 使用 `switchIfEmpty()` 处理空值情况

---

## 八、总结与扩展

### 8.1 核心要点回顾

1. **MCP 协议**：标准化的 AI 工具调用协议，实现客户端-服务器分离架构
2. **注解驱动**：通过 `@McpTool` 和 `@McpToolParam` 快速暴露工具能力
3. **响应式编程**：基于 WebFlux + R2DBC 的全链路异步非阻塞架构
4. **AI 集成**：结合 Spring AI 实现智能问答和数据分析

### 8.2 可扩展方向

- **认证授权**：添加 JWT 认证，保护 MCP 端点
- **工具发现**：实现动态工具注册和发现机制
- **监控告警**：集成 Micrometer 监控工具调用指标
- **多数据源**：支持 MySQL、MongoDB 等多种数据库
- **缓存优化**：集成 Redis 缓存热点数据

### 8.3 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [MCP 协议规范](https://modelcontextprotocol.io/)
- [Spring WebFlux 文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)

---

## 附录

### 版本信息

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.10 |
| Spring AI | 1.1.0-SNAPSHOT |
| JDK | 25 |
| PostgreSQL | 14+ |

---

**💰 为什么选择 32ai？**

**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

---

欢迎在评论区交流讨论！

**原创声明**：本文为原创教程，转载请注明出处
