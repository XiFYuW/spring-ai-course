# Spring AI 多模态实战：手把手教你构建图像理解应用

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-5](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-5)

## 引言

随着 GPT-4o、Claude 3、Gemini 等大模型的发布，**多模态 AI**（Multimodal AI）已经成为人工智能领域最热门的技术之一。多模态模型能够同时理解和处理文本、图像等多种类型的数据，为应用开发带来了无限可能。

**本文将带你从零开始，使用 Spring AI 构建一个功能完善的多模态图像分析应用**，涵盖图片内容分析、视觉问答、图片对比、结构化信息提取、OCR 文字识别等六大核心功能。

**读完本文，你将收获**：
- 深入理解 Spring AI 多模态 API 的设计与使用
- 掌握 Reactive 编程在 AI 应用中的实践
- 学会构建企业级的图像理解服务
- 了解多模态模型的应用场景和最佳实践

---

## 目录

- [一、项目概述与技术栈](#一项目概述与技术栈)
- [二、环境准备](#二环境准备)
- [三、核心概念解析](#三核心概念解析)
- [四、项目实战：从零开始构建](#四项目实战从零开始构建)
  - [4.1 项目初始化](#41-项目初始化)
  - [4.2 配置 Spring AI](#42-配置-spring-ai)
  - [4.3 实现多模态服务层](#43-实现多模态服务层)
  - [4.4 构建 REST API 控制器](#44-构建-rest-api-控制器)
  - [4.5 全局异常处理](#45-全局异常处理)
- [五、API 使用指南](#五api-使用指南)
- [六、避坑指南与最佳实践](#六避坑指南与最佳实践)
- [七、总结与扩展](#七总结与扩展)

---

## 一、项目概述与技术栈

### 1.1 项目功能一览

本项目实现了以下 **6 大核心功能**：

| 功能 | 端点 | 说明 |
|------|------|------|
| **单张图片分析** | `POST /api/multimodal/analyze` | 上传图片，AI 详细描述图片内容 |
| **视觉问答** | `POST /api/multimodal/vqa` | 针对图片回答特定问题 |
| **图片对比** | `POST /api/multimodal/compare` | 对比多张图片的异同 |
| **结构化信息提取** | `POST /api/multimodal/extract` | 从图片提取结构化数据（如发票信息） |
| **图片文字分析** | `POST /api/multimodal/text` | OCR + 理解，支持提取/总结/翻译 |
| **创意描述生成** | `POST /api/multimodal/creative` | 基于图片生成故事、诗歌、营销文案 |

### 1.2 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 25 | 开发语言 |
| **Spring Boot** | 3.5.10 | 应用框架 |
| **Spring AI** | 1.1.0-SNAPSHOT | AI 开发框架 |
| **OpenAI API** | - | 多模态模型服务 |
| **Project Reactor** | - | 响应式编程 |

### 1.3 项目结构

```
phase-5/
├── src/main/java/org/example/
│   ├── SpringAiJcStart.java              # 启动类
│   ├── controller/
│   │   └── MultimodalController.java     # REST API 控制器
│   ├── service/
│   │   └── MultimodalService.java        # 多模态业务服务
│   └── exception/
│       ├── ChatException.java            # 自定义业务异常
│       ├── ErrorResponse.java            # 统一错误响应
│       └── GlobalExceptionHandler.java   # 全局异常处理
├── src/main/resources/
│   └── application.yml                   # 配置文件
└── pom.xml                               # Maven 依赖
```

---

## 二、环境准备

### 2.1 前置要求

- **JDK 25** 或更高版本
- **Maven 3.8+**
- **OpenAI API Key**（或其他兼容的 AI 服务）

### 2.2 获取 API Key

本项目使用 OpenAI 兼容的 API 格式。你可以：

1. **使用 OpenAI 官方 API**：访问 [OpenAI Platform](https://platform.openai.com)
2. **使用国内中转服务**：如示例中的 `https://ai.32zi.com`

**💰 推荐选择 32ai**：
- **低至 0.56 : 1 比率**
- **快速访问**：[点击访问](https://ai.32zi.com) — 直连、无需魔法

---

## 三、核心概念解析

### 3.1 什么是多模态 AI？

**多模态 AI**（Multimodal AI）是指能够同时处理和理解多种类型数据（模态）的人工智能模型。传统的 AI 模型通常只处理单一模态：

- **NLP 模型**：只处理文本
- **CV 模型**：只处理图像
- **ASR 模型**：只处理语音

而多模态模型（如 GPT-4o、Claude 3）能够**同时理解文本和图像**，实现真正的"看图说话"。

### 3.2 Spring AI 多模态 API 设计

Spring AI 提供了简洁优雅的多模态 API：

```java
// 核心类：ChatClient
ChatClient chatClient = ChatClient.builder(chatModel).build();

// 构建多模态请求
String response = chatClient.prompt()
    .user(userSpec -> userSpec
        .text("请描述这张图片")           // 文本提示
        .media(MimeTypeUtils.IMAGE_PNG, imageResource)  // 图像输入
    )
    .call()                              // 调用模型
    .content();                          // 获取响应
```

**关键点**：
- `userSpec.text()`：设置文本提示词
- `userSpec.media()`：添加媒体（图片）数据
- 支持同时添加**多张图片**

### 3.3 Spring AI 支持的多模态模型

Spring AI 目前为以下聊天模型提供多模态支持：

| 厂商/平台 | 支持模型 | 特点 |
|-----------|----------|------|
| **OpenAI** | GPT-4o, GPT-4 Vision | 功能强大，识别准确，业界标杆 |
| **Anthropic** | Claude 3 (Opus/Sonnet/Haiku) | 上下文窗口长，理解能力强 |
| **Azure OpenAI** | GPT-4o, GPT-4 Turbo with Vision | 企业级服务，合规性好 |
| **Google Vertex AI** | Gemini 1.5 Pro/Flash | 多语言支持优秀，长上下文 |
| **AWS Bedrock** | Claude 3, Llama 3.2 | 云原生集成，按需付费 |
| **Mistral AI** | Pixtral | 欧洲开源模型，性能优秀 |
| **Ollama (本地)** | LLaVA, BakLLaVA, Llama 3.2 Vision | 可私有化部署，数据安全 |

**模型选择建议**：
- **追求效果**：OpenAI GPT-4o 或 Anthropic Claude 3 Opus
- **长文档分析**：Google Gemini 1.5 Pro（支持百万级上下文）
- **数据隐私**：Ollama + LLaVA（本地部署）
- **成本敏感**：AWS Bedrock 或 Mistral AI

---

## 四、项目实战：从零开始构建

### 4.1 项目初始化

#### 步骤 1：创建 Maven 项目

创建 `pom.xml` 文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>spring-ai-multimodal</artifactId>
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
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>1.1.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- WebFlux 响应式 Web 框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <!-- Spring MVC（排除 Tomcat，使用 Netty） -->
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
    </dependencies>
</project>
```

**关键依赖说明**：
- `spring-boot-starter-webflux`：响应式编程支持
- `spring-ai-starter-model-openai`：Spring AI OpenAI 集成

#### 步骤 2：创建启动类

```java
package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiJcStart {
    public static void main(String[] args) {
        SpringApplication.run(SpringAiJcStart.class, args);
    }
}
```

### 4.2 配置 Spring AI

创建 `src/main/resources/application.yml`：

```yaml
spring:
  http:
    codecs:
      max-in-memory-size: 10MB  # 增加文件上传大小限制
  ai:
    openai:
      api-key: your-api-key-here     # 替换为你的 API Key
      base-url: https://ai.32zi.com  # API 基础地址
      chat:
        options:
          model: claude-3-7-sonnet-20250219  # 多模态模型
      # 超时配置
      timeout:
        connect: 30s
        read: 120s
    # 重试配置
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000
        multiplier: 2
        max-interval: 10000
  server:
    port: 8080
    netty:
      connection-timeout: 60s
```

**配置要点**：
- `max-in-memory-size: 10MB`：允许上传更大的图片
- `timeout.read: 120s`：AI 响应可能需要较长时间
- `retry`：网络波动时自动重试

### 4.3 实现多模态服务层

创建 `MultimodalService.java`：

```java
package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class MultimodalService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalService.class);
    private final ChatClient chatClient;

    // 通过构造函数注入 ChatModel
    public MultimodalService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 分析单张图片
     */
    public Mono<String> analyzeImage(Resource imageResource, String question) {
        return Mono.fromCallable(() -> {
            logger.info("开始分析图片，问题: {}", question);

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(question != null ? question : "请详细描述这张图片中的内容")
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("图片分析完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 对比多张图片
     */
    public Mono<String> compareImages(List<Resource> imageResources, String comparisonPrompt) {
        return Mono.fromCallable(() -> {
            logger.info("开始对比 {} 张图片", imageResources.size());

            String response = chatClient.prompt()
                    .user(userSpec -> {
                        userSpec.text(comparisonPrompt != null ? comparisonPrompt 
                                : "请对比分析这些图片，找出它们的相似之处和差异。");
                        // 添加所有图片
                        for (Resource imageResource : imageResources) {
                            userSpec.media(MimeTypeUtils.IMAGE_PNG, imageResource);
                        }
                    })
                    .call()
                    .content();

            logger.info("图片对比完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 视觉问答
     */
    public Mono<String> visualQuestionAnswering(Resource imageResource, String question) {
        return Mono.fromCallable(() -> {
            logger.info("视觉问答，问题: {}", question);

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(question)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("视觉问答完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 提取结构化信息
     */
    public Mono<String> extractStructuredInfo(Resource imageResource, 
                                               String extractionPrompt,
                                               String outputFormat) {
        return Mono.fromCallable(() -> {
            logger.info("开始从图片提取结构化信息");

            String fullPrompt = String.format("""
                    %s
                    
                    请以以下格式输出结果：
                    %s
                    """, 
                    extractionPrompt != null ? extractionPrompt : "请分析这张图片并提取关键信息。",
                    outputFormat != null ? outputFormat : "{\"标题\": \"...\", \"主要内容\": \"...\"}"
            );

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(fullPrompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("结构化信息提取完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 分析图片中的文字
     */
    public Mono<String> analyzeImageText(Resource imageResource, String analysisType) {
        return Mono.fromCallable(() -> {
            logger.info("分析图片中的文字，类型: {}", analysisType);

            String prompt = switch (analysisType != null ? analysisType.toLowerCase() : "extract") {
                case "summarize" -> "请阅读图片中的文字内容，并提供简洁的摘要。";
                case "translate" -> "请将图片中的文字翻译成中文。";
                case "analyze" -> "请分析图片中的文字内容，解释其含义和背景。";
                default -> "请提取图片中的所有文字内容，保持原有格式。";
            };

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("图片文字分析完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 生成创意描述
     */
    public Mono<String> creativeDescription(Resource imageResource, String creativeStyle) {
        return Mono.fromCallable(() -> {
            logger.info("生成创意描述，风格: {}", creativeStyle);

            String prompt = switch (creativeStyle != null ? creativeStyle.toLowerCase() : "story") {
                case "poem" -> "请根据这张图片创作一首优美的诗歌。";
                case "marketing" -> "请为这张图片中的产品/场景撰写一段吸引人的营销文案。";
                case "social" -> "请为这张图片写一段适合社交媒体发布的配文，包含相关话题标签。";
                case "story" -> "请根据这张图片创作一个有趣的小故事。";
                default -> "请根据这张图片创作一段优美的描述性文字。";
            };

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("创意描述生成完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

**代码要点解析**：

1. **`ChatClient` 构建**：通过构造函数注入 `ChatModel`，构建 `ChatClient` 实例
2. **响应式编程**：使用 `Mono.fromCallable()` 包装阻塞调用，`subscribeOn(Schedulers.boundedElastic())` 确保在独立线程池执行
3. **多模态请求**：`userSpec.media(MimeTypeUtils.IMAGE_PNG, imageResource)` 添加图片输入
4. **多图片支持**：在 `compareImages` 中循环添加多张图片

### 4.4 构建 REST API 控制器

创建 `MultimodalController.java`：

```java
package org.example.controller;

import org.example.service.MultimodalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalController.class);
    private final MultimodalService multimodalService;

    public MultimodalController(MultimodalService multimodalService) {
        this.multimodalService = multimodalService;
    }

    /**
     * 1. 分析单张图片
     */
    @PostMapping("/analyze")
    public Mono<ResponseEntity<String>> analyzeImage(
            @RequestPart("image") FilePart image,
            @RequestPart("question") String question) {

        logger.info("收到图片分析请求，文件名: {}, 问题: {}", image.filename(), question);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.analyzeImage(imageResource, question)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("图片分析成功"))
                .doOnError(error -> logger.error("图片分析失败: {}", error.getMessage()));
    }

    /**
     * 2. 视觉问答
     */
    @PostMapping("/vqa")
    public Mono<ResponseEntity<String>> visualQuestionAnswering(
            @RequestPart("image") FilePart image,
            @RequestPart("question") String question) {

        logger.info("收到视觉问答请求，问题: {}", question);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.visualQuestionAnswering(imageResource, question)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok);
    }

    /**
     * 3. 对比多张图片
     */
    @PostMapping("/compare")
    public Mono<ResponseEntity<String>> compareImages(
            @RequestPart("images") List<FilePart> images,
            @RequestPart("prompt") String prompt) {

        logger.info("收到图片对比请求，图片数量: {}", images.size());

        if (images.size() < 2) {
            return Mono.just(ResponseEntity.badRequest()
                    .body("请至少上传两张图片进行对比"));
        }

        // 保存所有图片到临时文件
        List<Mono<Path>> tempPathMonos = images.stream()
                .map(this::saveFilePartToTemp)
                .toList();

        return Mono.zip(tempPathMonos, objects -> 
                    java.util.Arrays.stream(objects)
                            .map(obj -> (Path) obj)
                            .toList()
                )
                .flatMap(tempPaths -> {
                    List<Resource> imageResources = tempPaths.stream()
                            .map(path -> (Resource) new FileSystemResource(path.toFile()))
                            .toList();
                    
                    return multimodalService.compareImages(imageResources, prompt)
                            .doFinally(signal -> tempPaths.forEach(this::cleanupTempFile));
                })
                .map(ResponseEntity::ok);
    }

    /**
     * 4. 提取结构化信息
     */
    @PostMapping("/extract")
    public Mono<ResponseEntity<String>> extractStructuredInfo(
            @RequestPart("image") FilePart image,
            @RequestPart("prompt") String extractionPrompt,
            @RequestPart(value = "format", required = false) String outputFormat) {

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.extractStructuredInfo(imageResource, extractionPrompt, outputFormat)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok);
    }

    /**
     * 5. 分析图片中的文字
     */
    @PostMapping("/text")
    public Mono<ResponseEntity<String>> analyzeImageText(
            @RequestPart("image") FilePart image,
            @RequestPart(value = "type") String type) {

        logger.info("收到图片文字分析请求，类型: {}", type);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.analyzeImageText(imageResource, type)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok);
    }

    /**
     * 6. 生成创意描述
     */
    @PostMapping("/creative")
    public Mono<ResponseEntity<String>> creativeDescription(
            @RequestPart("image") FilePart image,
            @RequestPart(value = "style") String style) {

        logger.info("收到创意描述请求，风格: {}", style);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.creativeDescription(imageResource, style)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok);
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 FilePart 保存到临时文件
     */
    private Mono<Path> saveFilePartToTemp(FilePart filePart) {
        return Mono.fromCallable(() -> Files.createTempDirectory("multimodal_"))
                .flatMap(tempDir -> {
                    Path tempFile = tempDir.resolve(filePart.filename());
                    return filePart.transferTo(tempFile.toFile())
                            .then(Mono.fromCallable(() -> {
                                logger.debug("文件已保存到临时路径: {}", tempFile);
                                return tempFile;
                            }));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(Path path) {
        try {
            Files.deleteIfExists(path);
            Files.deleteIfExists(path.getParent());
            logger.debug("临时文件已清理: {}", path);
        } catch (Exception e) {
            logger.warn("清理临时文件失败: {}", path, e);
        }
    }
}
```

**关键技术点**：

1. **`@RequestPart` 注解**：用于接收 multipart/form-data 格式的文件上传
2. **`FilePart` 类型**：WebFlux 中处理文件上传的响应式类型
3. **临时文件处理**：使用 `saveFilePartToTemp()` 将上传的文件保存到临时目录
4. **`doFinally` 确保清理**：无论成功或失败，都会清理临时文件
5. **`Mono.zip` 并行处理**：在 `compareImages` 中同时保存多张图片

### 4.5 全局异常处理

创建统一异常处理类：

**ErrorResponse.java** - 错误响应实体：

```java
package org.example.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,           // HTTP状态码
        String error,         // 错误类型
        String message,       // 错误描述
        String path,          // 请求路径
        LocalDateTime timestamp  // 错误发生时间
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now());
    }
}
```

**ChatException.java** - 业务异常：

```java
package org.example.exception;

public class ChatException extends RuntimeException {
    public ChatException(String message) {
        super(message);
    }
    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**GlobalExceptionHandler.java** - 全局异常处理器：

```java
package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            ServerWebExchange exchange) {
        
        log.warn("参数错误: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ErrorResponse> handleChatException(
            ChatException ex, 
            ServerWebExchange exchange) {
        
        log.warn("业务错误: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            ServerWebExchange exchange) {
        
        log.error("服务器错误: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "服务器内部错误",
                exchange.getRequest().getPath().value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 五、API 使用指南

### 5.1 启动应用

```bash
mvn spring-boot:run
```

### 5.2 API 调用示例

#### 1. 分析单张图片

```bash
curl -X POST http://localhost:8080/api/multimodal/analyze \
  -F "image=@/path/to/your/image.png" \
  -F "question=这张图片里有什么？"
```

**响应示例**：
```
这张图片展示了一座现代化的城市天际线。画面中可以看到多栋高层建筑，
其中有几栋摩天大楼格外醒目。天空呈现出黄昏时分的橙红色调，
给整个场景增添了一种温暖而繁华的氛围...
```

#### 2. 视觉问答

```bash
curl -X POST http://localhost:8080/api/multimodal/vqa \
  -F "image=@/path/to/image.png" \
  -F "question=图中有几个人？"
```

#### 3. 对比多张图片

```bash
curl -X POST http://localhost:8080/api/multimodal/compare \
  -F "images=@/path/to/image1.png" \
  -F "images=@/path/to/image2.png" \
  -F "prompt=对比这两张图片的差异"
```

#### 4. 提取结构化信息

```bash
curl -X POST http://localhost:8080/api/multimodal/extract \
  -F "image=@/path/to/invoice.png" \
  -F "prompt=提取发票信息" \
  -F "format={\"金额\":\"...\",\"日期\":\"...\",\"商家\":\"...\"}"
```

#### 5. 图片文字分析

```bash
# 提取文字
curl -X POST "http://localhost:8080/api/multimodal/text?type=extract" \
  -F "image=@/path/to/document.png"

# 总结内容
curl -X POST "http://localhost:8080/api/multimodal/text?type=summarize" \
  -F "image=@/path/to/document.png"

# 翻译
curl -X POST "http://localhost:8080/api/multimodal/text?type=translate" \
  -F "image=@/path/to/document.png"
```

#### 6. 创意描述

```bash
# 生成诗歌
curl -X POST "http://localhost:8080/api/multimodal/creative?style=poem" \
  -F "image=@/path/to/image.png"

# 生成营销文案
curl -X POST "http://localhost:8080/api/multimodal/creative?style=marketing" \
  -F "image=@/path/to/product.png"

# 生成社交媒体配文
curl -X POST "http://localhost:8080/api/multimodal/creative?style=social" \
  -F "image=@/path/to/image.png"
```

---

## 六、避坑指南与最佳实践

### 6.1 常见问题与解决方案

#### 问题 1：文件上传大小限制

**现象**：上传大图片时报错 `Maximum size exceeded`

**解决**：在 `application.yml` 中增加配置：

```yaml
spring:
  http:
    codecs:
      max-in-memory-size: 10MB  # 根据需求调整
```

#### 问题 2：AI 响应超时

**现象**：调用 API 时超时

**解决**：增加超时配置：

```yaml
spring:
  ai:
    openai:
      timeout:
        connect: 30s
        read: 120s  # 图片分析可能需要较长时间
```

#### 问题 3：临时文件未清理

**现象**：磁盘空间持续增长

**解决**：确保使用 `doFinally` 清理资源：

```java
return multimodalService.analyzeImage(imageResource, question)
        .doFinally(signal -> cleanupTempFile(tempPath));  // 确保执行
```

### 6.2 最佳实践

1. **使用构造函数注入**：
   ```java
   // 推荐
   public MultimodalService(ChatModel chatModel) {
       this.chatClient = ChatClient.builder(chatModel).build();
   }
   ```

2. **响应式编程注意线程切换**：
   ```java
   return Mono.fromCallable(() -> {
       // 阻塞操作
   }).subscribeOn(Schedulers.boundedElastic());  // 在独立线程执行
   ```

3. **合理设置日志级别**：
   - 生产环境建议将 `org.springframework.ai` 设置为 WARN
   - 避免日志中泄露敏感信息（如 API Key）

4. **图片预处理**：
   - 大图片建议先压缩再上传，减少传输时间和 API 费用
   - 可以使用 `ImageIO` 进行格式转换和压缩

---

## 七、总结与扩展

### 7.1 项目回顾

本文详细介绍了如何使用 **Spring AI** 构建多模态图像分析应用，涵盖了：

- **6 大核心功能**：图片分析、视觉问答、图片对比、结构化提取、OCR、创意生成
- **响应式编程**：使用 WebFlux 和 Reactor 构建高性能异步应用
- **企业级实践**：全局异常处理、日志记录、资源清理

### 7.2 可扩展方向

基于本项目，你可以进一步实现：

1. **增加更多模态**：
   - 音频理解（语音识别 + 分析）
   - 视频分析（关键帧提取 + 时序理解）

2. **功能增强**：
   - 批量图片处理
   - 结果缓存（Redis）
   - 异步任务队列

3. **应用场景**：
   - **智能文档处理**：发票识别、合同审核
   - **电商应用**：商品图片自动标注、相似商品推荐
   - **内容审核**：图片合规性检查
   - **辅助工具**：图片转文字、自动生成图片描述

### 7.3 参考资料

- [Spring AI 官方文档 - 多模态](https://docs.springframework.org.cn/spring-ai/reference/api/multimodality.html)
- [OpenAI Vision API 文档](https://platform.openai.com/docs/guides/vision)
- [Project Reactor 文档](https://projectreactor.io/docs)

---

<!-- 互动提示 -->
欢迎在评论区交流讨论！如果你有任何问题或建议，欢迎留言。

**原创声明**：本文为原创教程，转载请注明出处。
