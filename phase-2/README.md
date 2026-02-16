# Spring AI 实战：手把手教你构建支持多会话管理的智能聊天服务

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-2](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-2)

## 引言

在 AI 应用爆发的今天，**多轮对话**和**会话管理**是构建生产级聊天应用的核心能力。然而，很多开发者在入门 Spring AI 时，往往只实现了简单的单次问答，缺乏对**上下文管理**、**会话持久化**、**流式输出**等关键特性的深入理解。

本文将带你从零开始，基于 **Spring Boot 3.x** + **Spring AI** + **R2DBC** 技术栈，构建一个**支持多会话管理、上下文记忆、流式/非流式双模式**的智能聊天服务。通过本文，你将掌握：

- 🎯 Spring AI 的核心 API 使用（ChatClient、Prompt、Message）
- 🔄 响应式编程在 AI 应用中的实践（Mono/Flux）
- 💾 基于 R2DBC 的会话和消息持久化方案
- 📊 上下文长度控制策略（防止 Token 超限）
- ⚡ SSE 流式输出的实现原理

---

## 目录

- [一、项目概述与技术选型](#一项目概述与技术选型)
- [二、环境准备](#二环境准备)
- [三、核心概念解析](#三核心概念解析)
- [四、项目结构搭建](#四项目结构搭建)
- [五、数据库设计与实体定义](#五数据库设计与实体定义)
- [六、配置类与属性绑定](#六配置类与属性绑定)
- [七、核心业务逻辑实现](#七核心业务逻辑实现)
- [八、RESTful API 设计](#八restful-api-设计)
- [九、API 测试与效果展示](#九api-测试与效果展示)
- [十、避坑指南与最佳实践](#十避坑指南与最佳实践)
- [十一、总结与扩展](#十一总结与扩展)

---

## 一、项目概述与技术选型

### 1.1 功能特性

本项目实现了一个完整的 AI 聊天服务，具备以下核心能力：

| 特性 | 说明 |
|------|------|
| **多会话管理** | 支持创建、查询、删除多个独立会话 |
| **上下文记忆** | 自动携带历史消息，支持多轮对话 |
| **上下文截断** | 智能控制历史消息数量，防止 Token 超限 |
| **双模式输出** | 支持同步响应（非流式）和 SSE 流式输出 |
| **响应式架构** | 全面使用 WebFlux + R2DBC，高并发友好 |
| **配置外部化** | 系统提示词、上下文长度等参数可配置 |

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.0.0-SNAPSHOT | AI 能力封装 |
| Spring WebFlux | 3.5.10 | 响应式 Web 框架 |
| Spring Data R2DBC | 3.5.10 | 响应式数据库访问 |
| PostgreSQL | 14+ | 数据持久化 |
| R2DBC PostgreSQL | - | 响应式 PostgreSQL 驱动 |
| Java | 25 | 编程语言 |

### 1.3 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      Client (Postman/Curl)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/SSE
┌───────────────────────▼─────────────────────────────────────┐
│              ChatController (RESTful API)                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ POST /chat  │ │POST /stream │ │  Session Management │   │
│  └──────┬──────┘ └──────┬──────┘ └──────────┬──────────┘   │
└─────────┼───────────────┼───────────────────┼──────────────┘
          │               │                   │
┌─────────▼───────────────▼───────────────────▼──────────────┐
│                    ChatService                              │
│         (业务逻辑 + 上下文管理 + 消息持久化)                  │
└─────────┬───────────────────────┬──────────────────────────┘
          │                       │
┌─────────▼──────────┐  ┌─────────▼──────────┐
│   ChatClient       │  │  R2DBC Repository  │
│  (Spring AI)       │  │ (Session/Message)  │
└─────────┬──────────┘  └─────────┬──────────┘
          │                       │
┌─────────▼──────────┐  ┌─────────▼──────────┐
│   OpenAI API       │  │   PostgreSQL       │
│  (或兼容 API)       │  │   (chatdb)         │
└────────────────────┘  └────────────────────┘
```

---

## 二、环境准备

### 2.1 前置条件

1. **JDK 25** 或更高版本
2. **Maven 3.8+**
3. **PostgreSQL 14+** 数据库
4. **AI API Key**（支持 OpenAI 或兼容的 API，如 32ai）

### 2.2 创建数据库

```sql
-- 创建数据库
CREATE DATABASE chatdb;

-- 创建用户（如需要）
CREATE USER chatuser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;
```

### 2.3 项目初始化

创建 Maven 项目，添加以下依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>spring-ai-chat</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
    </parent>

    <!-- Spring AI 仓库配置 -->
    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>https://repo.spring.io/snapshot</url>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
    </repositories>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- WebFlux - 响应式 Web 框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Spring MVC（可选，用于兼容） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        
        <!-- Spring AI OpenAI Starter -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
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
</project>
```

---

## 三、核心概念解析

在深入代码之前，先理解几个关键概念：

### 3.1 Spring AI 核心组件

| 组件 | 作用 |
|------|------|
| `ChatClient` | AI 聊天的核心入口，封装了模型调用逻辑 |
| `Prompt` | 提示词对象，包含完整的对话上下文 |
| `Message` | 单条消息，分为 `SystemMessage`、`UserMessage`、`AssistantMessage` |
| `ChatModel` | 底层模型接口，由 Spring AI 自动配置 |

### 3.2 响应式编程（Reactor）

Spring WebFlux 使用 **Reactor** 作为响应式编程库：

- **`Mono<T>`**：表示 0 或 1 个元素的异步序列（适合单条数据）
- **`Flux<T>`**：表示 0 到 N 个元素的异步序列（适合流式数据、列表）

**为什么用响应式？**
- 更高的并发处理能力（少量线程处理大量连接）
- 天然适合 SSE 流式输出
- 与 R2DBC 配合实现全链路异步

### 3.3 上下文长度控制策略

大模型有 **Token 限制**（如 GPT-3.5 是 4K/16K，GPT-4 是 8K/32K），需要控制发送的历史消息数量：

```
策略：滑动窗口，保留最近 N 轮对话

完整历史：[Msg1, Msg2, Msg3, ..., Msg50]  (50条 = 25轮)
                    ↓ 截断，保留最近 20 轮
发送给AI：[SystemMsg, Msg11, Msg12, ..., Msg50]  (41条 = 1系统+20轮)
```

---

## 四、项目结构搭建

```
src/main/java/org/example/
├── SpringAiJcStart.java              # 启动类
├── config/
│   └── ChatProperties.java           # 配置属性类
├── controller/
│   └── ChatController.java           # REST API 控制器
├── entity/
│   ├── ConversationSession.java      # 会话实体
│   └── ConversationMessage.java      # 消息实体
├── exception/
│   ├── ChatException.java            # 自定义异常
│   ├── ErrorResponse.java            # 错误响应
│   └── GlobalExceptionHandler.java   # 全局异常处理
├── repository/
│   ├── ConversationSessionRepository.java    # 会话数据访问
│   └── ConversationMessageRepository.java    # 消息数据访问
└── service/
    └── ChatService.java              # 核心业务逻辑

src/main/resources/
├── application.yml                   # 应用配置
└── schema.sql                        # 数据库初始化脚本
```

---

## 五、数据库设计与实体定义

### 5.1 数据库表结构

```sql
-- schema.sql
-- 创建会话表
CREATE TABLE IF NOT EXISTS conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,  -- 'user' 或 'assistant'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_messages_session_id ON conversation_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_sessions_updated_at ON conversation_sessions(updated_at DESC);
```

### 5.2 会话实体

```java
package org.example.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 会话实体 - 对应 conversation_sessions 表
 * 
 * 使用 Java Record 简化定义，自动包含：
 * - 构造方法
 * - getter 方法
 * - equals/hashCode/toString
 */
@Table("conversation_sessions")
public record ConversationSession(
    @Id
    Long id,
    
    @Column("title")
    String title,
    
    @Column("created_at")
    LocalDateTime createdAt,
    
    @Column("updated_at")
    LocalDateTime updatedAt
) {
    /**
     * 工厂方法：创建新会话
     */
    public static ConversationSession create(String title) {
        LocalDateTime now = LocalDateTime.now();
        return new ConversationSession(
            null,       // id 由数据库自动生成
            title,
            now,
            now
        );
    }
    
    /**
     * 更新时间戳
     */
    public ConversationSession withUpdatedTime() {
        return new ConversationSession(
            this.id,
            this.title,
            this.createdAt,
            LocalDateTime.now()
        );
    }
}
```

### 5.3 消息实体

```java
package org.example.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 消息实体 - 对应 conversation_messages 表
 */
@Table("conversation_messages")
public record ConversationMessage(
    @Id
    Long id,
    
    @Column("session_id")
    Long sessionId,
    
    @Column("role")
    String role,        // "user" 或 "assistant"
    
    @Column("content")
    String content,
    
    @Column("created_at")
    LocalDateTime createdAt
) {
    /**
     * 工厂方法：创建新消息
     */
    public static ConversationMessage of(Long sessionId, String role, String content) {
        return new ConversationMessage(
            null,
            sessionId,
            role,
            content,
            LocalDateTime.now()
        );
    }
}
```

### 5.4 Repository 接口

```java
package org.example.repository;

import org.example.entity.ConversationSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * 会话数据访问接口
 */
public interface ConversationSessionRepository 
    extends ReactiveCrudRepository<ConversationSession, Long> {
    
    /**
     * 按更新时间倒序查询所有会话
     */
    Flux<ConversationSession> findAllByOrderByUpdatedAtDesc();
}
```

```java
package org.example.repository;

import org.example.entity.ConversationMessage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
     * 消息数据访问接口
 */
public interface ConversationMessageRepository 
    extends ReactiveCrudRepository<ConversationMessage, Long> {
    
    /**
     * 按时间正序查询会话的所有消息
     */
    Flux<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    /**
     * 删除会话的所有消息
     */
    Mono<Void> deleteBySessionId(Long sessionId);
}
```

---

## 六、配置类与属性绑定

### 6.1 配置文件

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key-here}
      base-url: https://api.openai.com  # 或使用兼容 API
      chat:
        options:
          model: gpt-3.5-turbo
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000
        multiplier: 2
        max-interval: 10000
  
  server:
    port: 8080
  
  # R2DBC 数据库配置
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/chatdb
    username: postgres
    password: root
    pool:
      enabled: true
      initial-size: 5
      max-size: 20
  
  # 自动执行 schema.sql
  sql:
    init:
      mode: always

# 自定义配置
app:
  chat:
    # 系统提示词 - 定义AI助手的行为准则
    system-prompt: "你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"
    # 最大保留的对话轮数（一对 = user + assistant）
    max-context-pairs: 20
```

### 6.2 配置属性类

```java
package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天服务配置属性
 * 
 * 配置项前缀: app.chat
 * 支持在 application.yml 中自定义配置
 */
@Configuration
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /**
     * 系统提示词 - 定义AI助手的行为准则
     */
    private String systemPrompt = "你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。";

    /**
     * 最大保留的对话轮数（一对 = user + assistant）
     * 默认20对 = 40条消息，加上系统消息共41条
     */
    private int maxContextPairs = 20;

    // Getters and Setters
    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxContextPairs() {
        return maxContextPairs;
    }

    public void setMaxContextPairs(int maxContextPairs) {
        this.maxContextPairs = maxContextPairs;
    }

    /**
     * 获取最大保留的消息条数（不含系统消息）
     */
    public int getMaxContextMessages() {
        return maxContextPairs * 2;
    }
}
```

---

## 七、核心业务逻辑实现

### 7.1 ChatService 整体结构

```java
package org.example.service;

import org.example.config.ChatProperties;
import org.example.entity.ConversationMessage;
import org.example.entity.ConversationSession;
import org.example.repository.ConversationMessageRepository;
import org.example.repository.ConversationSessionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务 - 支持多会话管理和上下文长度控制
 * 
 * 核心设计原则：
 * 1. 无状态设计 - 不保存当前会话状态，所有操作都需要明确的 sessionId
 * 2. 上下文控制 - 限制历史消息数量，防止token超限
 * 3. 响应式编程 - 全面使用 Mono/Flux 进行异步操作
 * 4. 配置外部化 - 系统提示词和上下文长度通过配置文件管理
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final ChatProperties chatProperties;

    public ChatService(
            ChatModel chatModel,
            ConversationSessionRepository sessionRepository,
            ConversationMessageRepository messageRepository,
            ChatProperties chatProperties
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatProperties = chatProperties;
    }
    
    // ... 业务方法将在下文展开
}
```

### 7.2 非流式聊天实现

```java
/**
 * 普通聊天（非流式）
 * 
 * 执行流程：
 * 1. 验证 sessionId 有效性
 * 2. 构建对话历史（带上下文截断）
 * 3. 添加当前用户消息
 * 4. 调用 AI 模型
 * 5. 保存用户消息和 AI 回复到数据库
 * 6. 返回 AI 回复
 * 
 * @param sessionId 会话ID，必须提供
 * @param userMessage 用户消息
 * @return AI回复
 */
public Mono<String> chat(Long sessionId, String userMessage) {
    // 参数校验
    if (sessionId == null) {
        return Mono.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
    }
    
    return validateSessionExists(sessionId)
            .flatMap(sid -> buildConversationHistory(sid)
                    .flatMap(history -> {
                        // 添加用户消息到历史
                        history.add(new UserMessage(userMessage));
                        
                        // 构建 Prompt
                        Prompt prompt = new Prompt(new ArrayList<>(history));
                        
                        // 调用 AI（使用 boundedElastic 线程池避免阻塞）
                        return Mono.fromCallable(() -> 
                                chatClient.prompt(prompt)
                                        .call()
                                        .content()
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(response -> {
                            if (response != null && !response.isEmpty()) {
                                // 先保存用户消息，再保存AI回复
                                return saveMessage(sid, MessageType.USER.getValue(), userMessage)
                                        .then(saveMessage(sid, MessageType.ASSISTANT.getValue(), response))
                                        .thenReturn(response);
                            }
                            return Mono.justOrEmpty(response);
                        })
                        // 错误处理：AI 服务暂时不可用
                        .onErrorResume(TransientAiException.class, e -> {
                            String errorMsg = extractErrorMessage(e);
                            return Mono.just("【AI服务暂时不可用】" + errorMsg);
                        })
                        // 错误处理：其他异常
                        .onErrorResume(Exception.class, e -> 
                            Mono.just("【请求失败】" + e.getMessage())
                        );
                    })
            );
}
```

### 7.3 流式聊天实现（SSE）

```java
/**
 * 流式聊天（SSE）
 * 
 * 执行流程：
 * 1. 验证 sessionId 有效性
 * 2. 构建对话历史
 * 3. 添加当前用户消息
 * 4. 调用 AI 流式接口
 * 5. 实时返回流式数据
 * 6. 流完成后异步保存消息
 * 
 * @param sessionId 会话ID，必须提供
 * @param userMessage 用户消息
 * @return 流式AI回复（Flux<String>）
 */
public Flux<String> chatStream(Long sessionId, String userMessage) {
    if (sessionId == null) {
        return Flux.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
    }
    
    return validateSessionExists(sessionId)
            .flatMapMany(sid -> buildConversationHistory(sid)
                    .flatMapMany(history -> {
                        // 添加用户消息到历史
                        history.add(new UserMessage(userMessage));
                        
                        Prompt prompt = new Prompt(new ArrayList<>(history));
                        StringBuilder fullResponse = new StringBuilder();
                        
                        return chatClient.prompt(prompt)
                                .stream()           // 启用流式输出
                                .content()          // 获取内容流
                                .doOnNext(fullResponse::append)  // 累积完整回复
                                .doOnComplete(() -> {
                                    // 流完成后异步保存消息
                                    if (!fullResponse.isEmpty()) {
                                        saveMessage(sid, MessageType.USER.getValue(), userMessage)
                                                .then(saveMessage(sid, MessageType.ASSISTANT.getValue(), fullResponse.toString()))
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .subscribe();  // 异步执行，不阻塞响应
                                    }
                                })
                                .onErrorResume(TransientAiException.class, e -> {
                                    String errorMsg = extractErrorMessage(e);
                                    return Flux.just("【AI服务暂时不可用】" + errorMsg);
                                })
                                .onErrorResume(Exception.class, e -> 
                                    Flux.just("【请求失败】" + e.getMessage())
                                );
                    })
            );
}
```

### 7.4 上下文历史构建（核心逻辑）

```java
/**
 * 构建对话历史（带上下文长度控制）
 * 
 * 策略：
 * 1. 始终包含系统消息（从配置读取）
 * 2. 从数据库获取该会话的所有历史消息
 * 3. 如果消息数量超过限制，保留最近的 maxContextMessages 条
 * 
 * @param sessionId 会话ID
 * @return 构建好的消息列表（包含系统消息）
 */
private Mono<List<Message>> buildConversationHistory(Long sessionId) {
    return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .collectList()
            .map(messages -> {
                List<Message> history = new ArrayList<>();
                
                // 1. 添加系统消息（从配置读取）
                history.add(new SystemMessage(chatProperties.getSystemPrompt()));
                
                // 2. 处理历史消息（上下文截断）
                List<ConversationMessage> contextMessages = messages;
                int maxContextMessages = chatProperties.getMaxContextMessages();
                
                // 如果消息过多，只保留最近的 maxContextMessages 条
                if (messages.size() > maxContextMessages) {
                    contextMessages = messages.subList(
                            messages.size() - maxContextMessages, 
                            messages.size()
                    );
                }
                
                // 3. 转换为 Spring AI Message 对象
                for (ConversationMessage msg : contextMessages) {
                    if (MessageType.USER.getValue().equals(msg.role())) {
                        history.add(new UserMessage(msg.content()));
                    } else if (MessageType.ASSISTANT.getValue().equals(msg.role())) {
                        history.add(new AssistantMessage(msg.content()));
                    }
                    // 系统消息已在开头添加，数据库中的系统消息忽略
                }
                
                return history;
            });
}
```

### 7.5 会话管理方法

```java
/**
 * 创建新会话
 * 
 * @param title 会话标题（可选，默认为"新会话"）
 * @return 新会话ID
 */
public Mono<Long> createNewSession(String title) {
    String sessionTitle = (title == null || title.isBlank()) ? "新会话" : title;
    return sessionRepository.save(ConversationSession.create(sessionTitle))
            .map(ConversationSession::id);
}

/**
 * 获取所有会话列表（按更新时间倒序）
 * 
 * @return 会话列表
 */
public Flux<ConversationSession> getAllSessions() {
    return sessionRepository.findAllByOrderByUpdatedAtDesc();
}

/**
 * 获取指定会话的所有消息
 * 
 * @param sessionId 会话ID
 * @return 消息列表
 */
public Flux<ConversationMessage> getSessionMessages(Long sessionId) {
    if (sessionId == null) {
        return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
    }
    return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
}

/**
 * 删除会话及其所有消息
 * 
 * @param sessionId 要删除的会话ID
 * @return 操作完成信号
 */
public Mono<Void> deleteSession(Long sessionId) {
    if (sessionId == null) {
        return Mono.error(new IllegalArgumentException("sessionId 不能为空"));
    }
    
    // 先删除消息，再删除会话（利用外键级联删除也可）
    return messageRepository.deleteBySessionId(sessionId)
            .then(sessionRepository.deleteById(sessionId));
}

/**
 * 验证会话是否存在
 */
private Mono<Long> validateSessionExists(Long sessionId) {
    return sessionRepository.existsById(sessionId)
            .flatMap(exists -> {
                if (exists) {
                    return Mono.just(sessionId);
                }
                return Mono.error(new RuntimeException("会话不存在: " + sessionId));
            });
}

/**
 * 保存消息到数据库
 */
private Mono<Void> saveMessage(Long sessionId, String role, String content) {
    return messageRepository.save(ConversationMessage.of(sessionId, role, content))
            .then();
}
```

---

## 八、RESTful API 设计

### 8.1 控制器完整代码

```java
package org.example.controller;

import org.example.entity.ConversationMessage;
import org.example.entity.ConversationSession;
import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 聊天控制器 - RESTful API
 * 
 * API 设计原则：
 * 1. 所有聊天操作都需要提供 sessionId，明确指定操作哪个会话
 * 2. 会话管理与会话操作分离
 * 3. 支持流式和非流式两种聊天模式
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ==================== 聊天 API ====================

    /**
     * 普通聊天（非流式）
     * 
     * @param request 包含 sessionId 和 message
     * @return AI 回复
     */
    @PostMapping
    public Mono<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.sessionId(), request.message());
    }

    /**
     * 流式聊天（SSE）
     * 
     * @param request 包含 sessionId 和 message
     * @return 流式 AI 回复
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.sessionId(), request.message());
    }

    // ==================== 会话管理 API ====================

    /**
     * 创建新会话
     * 
     * @param request 可选的会话标题
     * @return 新创建的会话ID
     */
    @PostMapping("/sessions")
    public Mono<CreateSessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String title = (request != null) ? request.title() : null;
        return chatService.createNewSession(title)
                .map(CreateSessionResponse::new);
    }

    /**
     * 获取所有会话列表
     * 
     * @return 按更新时间倒序排列的会话列表
     */
    @GetMapping("/sessions")
    public Flux<ConversationSession> getAllSessions() {
        return chatService.getAllSessions();
    }

    /**
     * 获取指定会话的详细信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ConversationSession> getSession(@PathVariable Long sessionId) {
        return chatService.getSession(sessionId);
    }

    /**
     * 获取指定会话的所有消息
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Mono<List<ConversationMessage>> getSessionMessages(@PathVariable Long sessionId) {
        return chatService.getSessionMessages(sessionId).collectList();
    }

    /**
     * 删除会话及其所有消息
     * 
     * @param sessionId 要删除的会话ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<Void> deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId);
    }

    // ==================== 请求/响应记录 ====================

    /**
     * 聊天请求
     * 
     * @param sessionId 会话ID（必填）
     * @param message 用户消息
     */
    public record ChatRequest(Long sessionId, String message) {}

    /**
     * 创建会话请求
     * 
     * @param title 会话标题（可选）
     */
    public record CreateSessionRequest(String title) {}

    /**
     * 创建会话响应
     * 
     * @param sessionId 新创建的会话ID
     */
    public record CreateSessionResponse(Long sessionId) {}
}
```

### 8.2 API 端点汇总

| 方法 | 端点 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/chat` | 非流式聊天 | `{"sessionId": 1, "message": "你好"}` | `String` |
| POST | `/api/chat/stream` | 流式聊天（SSE） | `{"sessionId": 1, "message": "你好"}` | `Flux<String>` |
| POST | `/api/chat/sessions` | 创建会话 | `{"title": "会话标题"}`（可选） | `{"sessionId": 1}` |
| GET | `/api/chat/sessions` | 获取所有会话 | - | `Flux<ConversationSession>` |
| GET | `/api/chat/sessions/{id}` | 获取会话详情 | - | `ConversationSession` |
| GET | `/api/chat/sessions/{id}/messages` | 获取会话消息 | - | `List<ConversationMessage>` |
| DELETE | `/api/chat/sessions/{id}` | 删除会话 | - | `Void` |

---

## 九、API 测试与效果展示

### 9.1 启动应用

```bash
# 1. 确保 PostgreSQL 已启动且数据库已创建
# 2. 设置环境变量（或在 application.yml 中配置）
export OPENAI_API_KEY=your-api-key

# 3. 启动应用
mvn spring-boot:run
```

### 9.2 测试步骤

#### 步骤1：创建会话

```bash
curl -X POST http://localhost:8080/api/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "技术讨论"}'
```

**响应：**
```json
{"sessionId": 1}
```

[建议：此处插入 Postman 创建会话的截图]

#### 步骤2：非流式聊天

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "你好，请介绍一下 Spring AI"
  }'
```

**响应：**
```
你好！Spring AI 是 Spring 官方推出的 AI 应用开发框架...
```

#### 步骤3：流式聊天（SSE）

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "用 Java 写个 Hello World"
  }'
```

**响应（逐字输出）：**
```
当当然！以下是一...
```

[建议：此处插入流式输出的 GIF 动图，展示逐字输出效果]

#### 步骤4：多轮对话测试

```bash
# 第二轮对话（携带上下文）
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "刚才说的内容能再详细点吗？"
  }'
```

AI 能够根据之前的对话内容继续回答，证明上下文记忆生效。

#### 步骤5：查看会话历史

```bash
curl http://localhost:8080/api/chat/sessions/1/messages
```

**响应：**
```json
[
  {"id": 1, "sessionId": 1, "role": "user", "content": "你好，请介绍一下 Spring AI", ...},
  {"id": 2, "sessionId": 1, "role": "assistant", "content": "你好！Spring AI 是...", ...},
  {"id": 3, "sessionId": 1, "role": "user", "content": "刚才说的内容能再详细点吗？", ...},
  {"id": 4, "sessionId": 1, "role": "assistant", "content": "当然！Spring AI 提供了...", ...}
]
```

---

## 十、避坑指南与最佳实践

### 10.1 常见问题与解决方案

#### 问题1：R2DBC 连接失败

**现象：**
```
Cannot connect to localhost:5432
```

**解决方案：**
1. 检查 PostgreSQL 是否启动
2. 检查数据库 `chatdb` 是否已创建
3. 检查用户名密码是否正确
4. 检查 `schema.sql` 中的表名与实体类 `@Table` 注解是否一致

#### 问题2：AI 调用阻塞主线程

**现象：** 请求响应慢，并发量高时系统卡顿

**解决方案：**
```java
// 使用 Schedulers.boundedElastic() 避免阻塞
return Mono.fromCallable(() -> 
        chatClient.prompt(prompt).call().content()
)
.subscribeOn(Schedulers.boundedElastic())  // 在弹性线程池执行
```

#### 问题3：Token 超限错误

**现象：**
```
This model's maximum context length is 4097 tokens
```

**解决方案：**
- 调整 `app.chat.max-context-pairs` 配置，减少保留的历史轮数
- 或者使用支持更长上下文的模型（如 GPT-4-32K）

#### 问题4：流式输出乱码或格式错误

**现象：** SSE 流在浏览器中显示异常

**解决方案：**
- 确保注解正确：`@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)`
- 前端使用 `EventSource` 正确解析 SSE 格式

### 10.2 最佳实践

1. **始终校验 sessionId**：避免空指针和无效会话操作
2. **异步保存消息**：流式输出时，消息保存应异步执行，不阻塞响应
3. **合理的超时设置**：
   ```yaml
   server:
     netty:
       connection-timeout: 2s
   ```
4. **错误降级处理**：使用 `onErrorResume` 提供友好的错误提示
5. **敏感信息保护**：API Key 等配置使用环境变量，避免硬编码

---

## 十一、总结与扩展

### 11.1 核心知识点回顾

通过本文，我们学习了：

1. **Spring AI 基础**：`ChatClient`、`Prompt`、`Message` 的使用
2. **响应式编程**：`Mono` 和 `Flux` 在 AI 应用中的实践
3. **上下文管理**：滑动窗口策略控制历史消息数量
4. **流式输出**：SSE 协议的实现与应用
5. **数据持久化**：R2DBC + PostgreSQL 响应式数据库访问

### 11.2 可扩展方向

基于本项目，你可以进一步实现：

| 功能 | 实现思路 |
|------|----------|
| **用户认证** | 集成 Spring Security，会话关联用户ID |
| **消息分页** | 使用 `Pageable` 实现历史消息分页加载 |
| **多模型支持** | 配置多个 `ChatModel`，支持切换不同 AI |
| **消息编辑/重发** | 支持修改历史消息并重新生成回复 |
| **文件上传** | 集成 Spring AI 的文档解析能力 |
| **前端界面** | 使用 Vue/React 构建聊天界面 |

### 11.3 完整代码仓库

[建议：此处插入 GitHub/GitCode 仓库链接]

```
https://github.com/yourusername/spring-ai-chat-service
```

---

## 附录

### A. 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [R2DBC 官方文档](https://r2dbc.io/)
- [Server-Sent Events 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)

### B. 相关依赖版本对照

| 依赖 | 版本 |
|------|------|
| Spring Boot | 3.5.10 |
| Spring AI | 1.0.0-SNAPSHOT |
| Java | 25 |
| PostgreSQL | 14+ |

---

**原创声明**：本文为原创教程，转载请注明出处。

欢迎在评论区交流讨论！如果你有任何问题或建议，欢迎留言。

---

💰 **为什么选择 32ai？**

**低至 0.56 : 1 比率**  
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。
