# Spring AI 实战：手把手教你构建智能对话助手（支持流式输出）

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-1](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-1)

## 引言

在 AI 大模型爆发的今天，如何快速将 AI 能力集成到自己的应用中，成为每个开发者必须掌握的技能。Spring AI 作为 Spring 生态中专门用于 AI 集成的框架，提供了统一、优雅的 API 来对接各种大模型。

本文将带你从零开始，使用 **Spring Boot 3.5 + Spring AI** 构建一个完整的智能对话助手，支持**普通对话**和**流式输出**两种模式，并实现**对话历史管理**功能。读完本文，你将掌握 Spring AI 的核心用法，能够快速将 AI 能力应用到自己的项目中。

---

## 目录

- [一、技术栈与环境准备](#一技术栈与环境准备)
- [二、Spring AI 核心概念解析](#二spring-ai-核心概念解析)
- [三、项目结构搭建](#三项目结构搭建)
- [四、核心代码实现](#四核心代码实现)
- [五、接口测试与效果展示](#五接口测试与效果展示)
- [六、常见问题与避坑指南](#六常见问题与避坑指南)
- [七、总结与扩展思考](#七总结与扩展思考)

---

## 一、技术栈与环境准备

### 1.1 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 25 | Java 开发环境 |
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.0.0-SNAPSHOT | AI 集成框架 |
| Spring WebFlux | - | 响应式 Web 框架 |
| Lombok | - | 简化代码 |
| Maven | - | 项目构建工具 |

### 1.2 为什么选择 Spring AI？

Spring AI 的核心优势：

- **统一 API**：一套代码可对接 OpenAI、Azure、Ollama、讯飞星火等多种模型
- **Spring 生态集成**：完美融入 Spring Boot，配置简单，开箱即用
- **响应式支持**：原生支持 WebFlux，轻松实现流式输出
- **Prompt 模板**：提供强大的 Prompt 管理能力

### 1.3 环境要求

1. **JDK 17+** 已安装并配置环境变量
2. **Maven 3.6+** 或使用 IDE 内置 Maven
3. **AI 模型 API Key**（本文使用 OpenAI 兼容接口）

> 💡 **提示**：如果你没有 OpenAI 官方 API Key，可以使用兼容 OpenAI API 格式的第三方服务，如本文示例中的 `ai.32zi.com`，支持 Claude、GPT 等多种模型。

---

## 二、Spring AI 核心概念解析

在开始编码前，我们需要理解几个核心概念：

### 2.1 ChatClient

`ChatClient` 是 Spring AI 提供的高级 API，用于与 AI 模型进行交互。它支持：

- **同步调用**：等待完整响应返回
- **流式调用**：逐字返回响应内容

### 2.2 Message 类型

Spring AI 定义了四种消息类型：

| 消息类型 | 说明 |
|----------|------|
| `SystemMessage` | 系统提示词，定义 AI 的角色和行为规范 |
| `UserMessage` | 用户输入的消息 |
| `AssistantMessage` | AI 助手的回复 |
| `FunctionMessage` | 函数调用的结果 |

### 2.3 Prompt

`Prompt` 是消息的集合，用于构建完整的对话上下文。通过将历史消息加入 Prompt，可以实现多轮对话。

---

## 三、项目结构搭建

### 3.1 创建 Maven 项目

使用 IDE 或命令行创建一个 Maven 项目，项目结构如下：

```
spring-ai-jc/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/example/
│       │       ├── SpringAiJcStart.java      # 启动类
│       │       ├── controller/
│       │       │   └── ChatController.java   # 控制器
│       │       └── service/
│       │           └── ChatService.java      # 服务层
│       └── resources/
│           └── application.yml               # 配置文件
└── pom.xml
```

### 3.2 配置 pom.xml 依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>spring-ai-jc</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- 继承 Spring Boot 父项目 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
    </parent>

    <!-- 配置 Spring 仓库（Spring AI 尚未发布到中央仓库） -->
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

    <!-- Spring AI BOM 统一版本管理 -->
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
        <!-- WebFlux 响应式支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Web 支持（排除 Tomcat，使用 Netty） -->
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
        
        <!-- Lombok 简化代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Spring AI OpenAI 模块 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
    </dependencies>
</project>
```

> ⚠️ **注意**：Spring AI 目前处于快照版本，需要添加 Spring 仓库才能下载依赖。如果下载缓慢，可以配置国内 Maven 镜像。

### 3.3 配置 application.yml

```yaml
spring:
  ai:
    openai:
      # API Key（请替换为你自己的 Key）
      api-key: sk-your-api-key-here
      # API 基础地址（使用兼容 OpenAI 格式的服务）
      base-url: https://ai.32zi.com
      chat:
        options:
          # 指定使用的模型
          model: claude-3-haiku-20240307
  server:
    port: 8080
    netty:
      connection-timeout: 2s
```

> 💡 **小贴士**：`base-url` 可以替换为任何兼容 OpenAI API 格式的服务地址，如官方 OpenAI、Azure OpenAI、本地 Ollama 等。

---

## 四、核心代码实现

### 4.1 创建启动类

```java
package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiJcStart {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SpringAiJcStart.class);
        springApplication.run(args);
    }
}
```

### 4.2 实现服务层 ChatService

这是项目的核心类，负责与 AI 模型交互和管理对话历史：

```java
package org.example.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final List<Message> conversationHistory;

    public ChatService(ChatModel chatModel) {
        // 构建 ChatClient
        this.chatClient = ChatClient.builder(chatModel).build();
        // 初始化对话历史，添加系统提示词
        this.conversationHistory = new ArrayList<>();
        this.conversationHistory.add(new SystemMessage("你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"));
    }

    /**
     * 普通对话模式 - 同步返回完整响应
     */
    public Mono<String> chat(String userMessage) {
        // 添加用户消息到历史
        conversationHistory.add(new UserMessage(userMessage));
        Prompt prompt = new Prompt(conversationHistory);
        
        return Mono.fromCallable(() -> 
                chatClient.prompt(prompt)
                        .call()
                        .content()
        ).doOnNext(response -> {
            // 将 AI 回复添加到历史
            if (response != null && !response.isEmpty()) {
                conversationHistory.add(new AssistantMessage(response));
            }
        });
    }

    /**
     * 流式对话模式 - 逐字返回响应
     */
    public Flux<String> chatStream(String userMessage) {
        conversationHistory.add(new UserMessage(userMessage));
        Prompt prompt = new Prompt(conversationHistory);
        
        StringBuilder fullResponse = new StringBuilder();
        
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 流式完成后，将完整响应添加到历史
                    if (!fullResponse.isEmpty()) {
                        conversationHistory.add(new AssistantMessage(fullResponse.toString()));
                    }
                });
    }

    /**
     * 清空对话历史
     */
    public void clearHistory() {
        conversationHistory.clear();
        conversationHistory.add(new SystemMessage("你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"));
    }

    /**
     * 获取当前对话历史
     */
    public List<Message> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
}
```

**核心逻辑解析：**

1. **对话历史管理**：使用 `List<Message>` 存储对话历史，包括系统提示词、用户消息和 AI 回复
2. **同步模式**：使用 `chatClient.call()` 获取完整响应，适合短对话场景
3. **流式模式**：使用 `chatClient.stream()` 逐字返回，适合长文本生成，用户体验更好
4. **响应式编程**：返回 `Mono` 和 `Flux` 类型，支持非阻塞处理

### 4.3 实现控制器 ChatController

```java
package org.example.controller;

import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 普通对话接口
     */
    @PostMapping
    public Mono<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.message());
    }

    /**
     * 流式对话接口 - SSE 方式返回
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.message());
    }

    /**
     * 清空对话历史
     */
    @DeleteMapping("/history")
    public Mono<Void> clearHistory() {
        chatService.clearHistory();
        return Mono.empty();
    }

    /**
     * 请求体定义
     */
    public record ChatRequest(String message) {}
}
```

**关键点说明：**

- `MediaType.TEXT_EVENT_STREAM_VALUE`：指定 SSE（Server-Sent Events）响应类型，实现流式输出
- 使用 `record` 定义请求体，简洁优雅（JDK 16+ 特性）
- RESTful 风格接口设计，职责清晰

---

## 五、接口测试与效果展示

### 5.1 启动项目

运行 `SpringAiJcStart` 主类，看到以下日志表示启动成功：

```
Started SpringAiJcStart in 2.345 seconds
```

### 5.2 测试普通对话接口

使用 curl 或 Postman 测试：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下 Spring AI"}'
```

**响应示例：**

```json
Spring AI 是 Spring 生态系统中专门用于 AI 集成的框架。它提供了统一的 API 来对接各种大语言模型，如 OpenAI、Azure OpenAI、Ollama 等。主要特点包括：1. 统一的编程模型；2. 支持 Prompt 模板；3. 支持向量数据库集成；4. 原生支持响应式编程。
```

### 5.3 测试流式对话接口

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "请写一首关于春天的诗"}'
```

**效果说明：** 响应会逐字返回，用户可以看到文字逐渐出现的效果，体验更流畅。


### 5.4 测试多轮对话

连续发送多条消息，AI 会基于上下文进行回复：

```bash
# 第一轮
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "我叫小明"}'

# 第二轮
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你还记得我的名字吗？"}'
```

### 5.5 清空对话历史

```bash
curl -X DELETE http://localhost:8080/api/chat/history
```

---

## 六、常见问题与避坑指南

### 6.1 依赖下载失败

**问题**：Maven 无法下载 Spring AI 相关依赖。

**解决方案**：
1. 确保添加了 Spring 仓库配置
2. 检查网络连接，必要时配置代理或镜像
3. 清理本地仓库缓存：`mvn clean`

### 6.2 API Key 无效

**问题**：启动后调用接口返回 401 或 403 错误。

**解决方案**：
1. 检查 `application.yml` 中的 `api-key` 是否正确
2. 确认 `base-url` 配置是否与 API Key 匹配
3. 部分 API 服务需要充值或验证才能使用

### 6.3 流式输出乱码

**问题**：流式接口返回中文乱码。

**解决方案**：
1. 确保请求头包含 `Content-Type: application/json; charset=UTF-8`
2. 检查响应编码设置

### 6.4 对话上下文丢失

**问题**：多轮对话时 AI 无法记住之前的内容。

**解决方案**：
1. 确保没有误调用 `clearHistory()` 方法
2. 检查 `conversationHistory` 是否正确添加消息
3. 注意对话历史过长可能导致 Token 超限，需要实现历史截断策略

### 6.5 响应超时

**问题**：AI 响应时间过长导致请求超时。

**解决方案**：
1. 增加 `server.netty.connection-timeout` 配置
2. 优先使用流式接口，避免长时间等待
3. 选择响应速度更快的模型（如 `claude-3-haiku`）

---

## 七、总结与扩展思考

### 7.1 本文小结

我们通过本文学习了：

1. **Spring AI 框架基础**：理解了 ChatClient、Message、Prompt 等核心概念
2. **项目搭建**：完成了 Spring Boot + Spring AI 项目配置
3. **核心功能实现**：实现了普通对话和流式对话两种模式
4. **对话历史管理**：支持多轮对话上下文保持

### 7.2 扩展方向

基于本项目，你可以继续探索：

| 扩展方向 | 说明 |
|----------|------|
| **持久化存储** | 将对话历史保存到数据库，支持历史会话恢复 |
| **多模型切换** | 实现动态切换不同的 AI 模型 |
| **Prompt 模板** | 使用 Spring AI 的 Prompt Template 功能管理复杂提示词 |
| **RAG 增强** | 结合向量数据库实现知识库问答 |
| **Function Calling** | 让 AI 调用外部工具和 API |
| **前端界面** | 开发 Web 或移动端界面，提供更好的用户体验 |

### 7.3 学习资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Project Reactor 文档](https://projectreactor.io/docs)

---

## 💰 为什么选择 32ai？

**低至 0.56 : 1 比率**  
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

---

欢迎在评论区交流讨论！

**原创声明**：本文为原创教程，转载请注明出处
