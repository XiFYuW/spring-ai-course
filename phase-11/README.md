# Spring AI + Elasticsearch 向量存储实战：从零构建智能文档检索系统

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-11](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-11)

## 引言

在人工智能时代，**RAG（Retrieval-Augmented Generation，检索增强生成）** 已成为大模型应用的核心架构。而向量数据库作为 RAG 的"记忆中枢"，承担着语义检索的关键职责。

本文将带你手把手实现一个基于 **Spring AI** 和 **Elasticsearch** 的向量存储系统。通过本教程，你将学会：
- 🚀 如何快速集成 Spring AI 与 Elasticsearch
- 🔍 实现文档的语义相似性搜索
- 🎯 掌握带过滤条件的高级检索技巧
- 💡 理解向量存储在 AI 应用中的核心价值

无论你是想构建企业知识库、智能客服，还是文档问答系统，这篇文章都能为你打下坚实基础。

---

## 目录

- [一、技术背景与核心概念](#一技术背景与核心概念)
- [二、环境准备](#二环境准备)
- [三、项目搭建](#三项目搭建)
- [四、核心代码实现](#四核心代码实现)
- [五、API 接口详解](#五api-接口详解)
- [六、测试与效果展示](#六测试与效果展示)
- [七、常见问题与解决方案](#七常见问题与解决方案)
- [八、总结与扩展](#八总结与扩展)

---

## 一、技术背景与核心概念

### 1.1 什么是向量存储？

向量存储（Vector Store）是一种专门用于存储和检索**高维向量**的数据库。在 AI 应用中，文本、图像等数据会被 Embedding 模型转换为固定维度的向量（如 1536 维），这些向量捕获了数据的语义信息。

**核心优势**：
- **语义检索**：基于含义而非关键词匹配
- **相似度计算**：通过余弦相似度、欧氏距离等算法找到相关内容
- **支持多模态**：文本、图像、音频均可向量化

### 1.2 Spring AI 简介

Spring AI 是 Spring 官方推出的 AI 应用开发框架，它提供了：
- 统一的 AI 模型调用接口（支持 OpenAI、Azure、Ollama 等）
- 便捷的向量存储抽象层
- 开箱即用的 RAG 组件

### 1.3 为什么选择 Elasticsearch？

| 特性 | 说明 |
|------|------|
| 全文搜索 + 向量搜索 | 同时支持传统关键词和语义检索 |
| 分布式架构 | 天然支持水平扩展 |
| 成熟生态 | 丰富的监控、安全、运维工具 |
| Spring 原生支持 | 完美集成 Spring Data Elasticsearch |

---

## 二、环境准备

### 2.1 所需环境

| 组件 | 版本要求 |
|------|----------|
| JDK | 25+ |
| Maven | 3.8+ |
| Spring Boot | 3.5.10 |
| Elasticsearch | 8.x |
| OpenAI API | 兼容接口 |

### 2.2 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 2.3 启动 Elasticsearch

推荐使用 Docker 快速启动：

```bash
# 创建网络
docker network create elastic

# 启动 Elasticsearch
docker run -d \
  --name elasticsearch \
  --net elastic \
  -p 9200:9200 \
  -p 5600:5600 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.0
```

windows启动：: elasticsearch-8.13.4\bin\elasticsearch.bat

> **避坑提示**：确保 Elasticsearch 的 `xpack.security.enabled` 设置为 `false`，否则需要配置用户名密码。

---

## 三、项目搭建

### 3.1 创建 Maven 项目

项目结构如下：

```
spring-ai-jc/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── org/
        │       └── example/
        │           ├── SpringAiJcStart.java
        │           ├── controller/
        │           │   └── VectorStoreController.java
        │           └── service/
        │               └── VectorStoreService.java
        └── resources/
            └── application.yml
```

[建议：此处插入项目结构截图]

### 3.2 配置 pom.xml

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
        <!-- Spring AI OpenAI 模型支持 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        <!-- Spring AI Elasticsearch 向量存储 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-elasticsearch</artifactId>
        </dependency>
    </dependencies>
</project>
```

> **关键点解析**：
> - `spring-ai-bom` 统一管理 Spring AI 依赖版本
> - `spring-ai-starter-vector-store-elasticsearch` 是核心依赖，提供向量存储能力
> - 使用 WebFlux 而非传统 MVC，支持响应式编程

### 3.3 配置 application.yml

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-api-key-here
      base-url: https://ai.32zi.com
      chat:
        options:
          model: claude-haiku-4-5-20251001
    vectorstore:
      elasticsearch:
        # 是否初始化所需的模式（索引）
        initialize-schema: true
        # 存储向量的索引名称
        index-name: spring-ai-document-index
        # 向量的维数（根据使用的 Embedding 模型调整，OpenAI text-embedding-ada-002 是 1536）
        dimensions: 1536
        # 相似性函数：cosine（默认）、l2_norm、dot_product
        similarity: cosine
        # 向量字段名称
        embedding-field-name: embedding
  elasticsearch:
    # Elasticsearch 实例地址
    uris: http://localhost:5600
    # 连接超时时间
    connection-timeout: 10s
    # Socket 超时时间
    socket-timeout: 30s
    # 是否启用 socket keep alive
    socket-keep-alive: true

server:
  port: 8080
```

> **配置要点**：
> - `initialize-schema: true` 会自动创建 Elasticsearch 索引
> - `dimensions: 1536` 需与 Embedding 模型输出维度匹配
> - `similarity: cosine` 是语义搜索最常用的相似度算法

---

## 四、核心代码实现

### 4.1 启动类

```java
package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class SpringAiJcStart {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SpringAiJcStart.class);
        springApplication.run(args);
    }

}
```

### 4.2 向量存储服务层

`VectorStoreService.java` 是业务逻辑的核心：

```java
package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务类
 * 
 * 提供文档的添加、搜索、删除等操作
 * 基于 Elasticsearch 向量存储实现
 */
@Service
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 添加文档到向量存储
     * 
     * @param content 文档内容
     * @param metadata 文档元数据
     * @return 操作结果
     */
    public Mono<Void> addDocument(String content, Map<String, Object> metadata) {
        return Mono.fromRunnable(() -> {
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
            logger.info("Document added to vector store: {}", 
                content.substring(0, Math.min(50, content.length())));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 批量添加文档
     * 
     * @param documents 文档列表
     * @return 操作结果
     */
    public Mono<Void> addDocuments(List<Document> documents) {
        return Mono.fromRunnable(() -> {
            vectorStore.add(documents);
            logger.info("Batch added {} documents to vector store", documents.size());
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 相似性搜索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    public Mono<List<Document>> similaritySearch(String query, int topK) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Similarity search for '{}' returned {} results", query, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 相似性搜索（带相似度阈值）
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值（0.0 - 1.0）
     * @return 相似文档列表
     */
    public Mono<List<Document>> similaritySearch(String query, int topK, double similarityThreshold) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Similarity search for '{}' with threshold {} returned {} results", 
                    query, similarityThreshold, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 根据表达式搜索文档
     * 
     * @param query 查询文本
     * @param filterExpression 过滤表达式（如 "meta1 == 'value1'"）
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    public Mono<List<Document>> searchWithFilter(String query, String filterExpression, int topK) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(filterExpression)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Filtered search for '{}' with filter '{}' returned {} results", 
                    query, filterExpression, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除所有文档
     * 
     * @return 操作结果
     */
    public Mono<Boolean> deleteAll() {
        return Mono.fromCallable(() -> {
            vectorStore.delete(List.of());
            logger.info("All documents deleted from vector store");
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 根据ID删除文档
     * 
     * @param ids 文档ID列表
     * @return 操作结果
     */
    public Mono<Boolean> deleteByIds(List<String> ids) {
        return Mono.fromCallable(() -> {
            vectorStore.delete(ids);
            logger.info("Deleted {} documents from vector store", ids.size());
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

> **代码解析**：
> - `VectorStore` 是 Spring AI 提供的抽象接口，底层自动使用 Elasticsearch
> - `SearchRequest.builder()` 构建灵活的搜索请求
> - `Schedulers.boundedElastic()` 确保阻塞 IO 操作不会阻塞事件循环

### 4.3 REST API 控制器

`VectorStoreController.java` 提供 HTTP 接口：

```java
package org.example.controller;

import org.example.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 向量存储 REST API 控制器
 * 
 * 提供文档的增删改查接口，基于 Elasticsearch 向量存储
 */
@RestController
@RequestMapping("/api/vector-store")
public class VectorStoreController {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreController.class);

    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 添加文档
     * 
     * POST /api/vector-store/documents
     */
    @PostMapping("/documents")
    public Mono<ResponseEntity<ApiResponse<Void>>> addDocument(@RequestBody AddDocumentRequest request) {
        logger.info("Adding document: {}", 
            request.content().substring(0, Math.min(50, request.content.length())));

        return vectorStoreService.addDocument(request.content(), request.metadata())
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>success("Document added successfully", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to add document", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to add document: " + e.getMessage())));
                });
    }

    /**
     * 批量添加文档
     * 
     * POST /api/vector-store/documents/batch
     */
    @PostMapping("/documents/batch")
    public Mono<ResponseEntity<ApiResponse<Void>>> addDocuments(@RequestBody List<AddDocumentRequest> requests) {
        logger.info("Batch adding {} documents", requests.size());

        List<Document> documents = requests.stream()
                .map(req -> new Document(req.content(), req.metadata()))
                .toList();

        return vectorStoreService.addDocuments(documents)
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>success("Batch documents added successfully", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to batch add documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to batch add documents: " + e.getMessage())));
                });
    }

    /**
     * 相似性搜索
     * 
     * GET /api/vector-store/search?query=Spring&topK=5
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        logger.info("Searching for: {}, topK: {}", query, topK);
        
        return vectorStoreService.similaritySearch(query, topK)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Search failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Search failed: " + e.getMessage())));
                });
    }

    /**
     * 带相似度阈值的搜索
     * 
     * GET /api/vector-store/search/threshold?query=Spring&topK=5&threshold=0.8
     */
    @GetMapping("/search/threshold")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> searchWithThreshold(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.0") double threshold) {
        logger.info("Searching for: {}, topK: {}, threshold: {}", query, topK, threshold);
        
        return vectorStoreService.similaritySearch(query, topK, threshold)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Search with threshold failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Search failed: " + e.getMessage())));
                });
    }

    /**
     * 带过滤条件的搜索
     * 
     * GET /api/vector-store/search/filter?query=Spring&filter=category=='technology'&topK=5
     */
    @GetMapping("/search/filter")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> searchWithFilter(
            @RequestParam String query,
            @RequestParam String filter,
            @RequestParam(defaultValue = "5") int topK) {
        logger.info("Searching for: {} with filter: {}, topK: {}", query, filter, topK);
        
        return vectorStoreService.searchWithFilter(query, filter, topK)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Filtered search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Filtered search failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Filtered search failed: " + e.getMessage())));
                });
    }

    /**
     * 删除所有文档
     * 
     * DELETE /api/vector-store/documents
     */
    @DeleteMapping("/documents")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAll() {
        logger.info("Deleting all documents");

        return vectorStoreService.deleteAll()
                .map(success -> ResponseEntity.ok(ApiResponse.<Void>success("All documents deleted", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to delete all documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to delete documents: " + e.getMessage())));
                });
    }

    /**
     * 根据ID删除文档
     * 
     * DELETE /api/vector-store/documents/ids
     */
    @DeleteMapping("/documents/ids")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteByIds(@RequestBody List<String> ids) {
        logger.info("Deleting documents by IDs: {}", ids);

        return vectorStoreService.deleteByIds(ids)
                .map(success -> ResponseEntity.ok(ApiResponse.<Void>success("Documents deleted", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to delete documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to delete documents: " + e.getMessage())));
                });
    }

    // ==================== 请求/响应记录类 ====================

    public record AddDocumentRequest(String content, Map<String, Object> metadata) {
        public AddDocumentRequest {
            if (metadata == null) {
                metadata = Map.of();
            }
        }
    }

    public record DocumentResponse(String id, String content, Map<String, Object> metadata) {}

    public record ApiResponse<T>(boolean success, String message, T data) {
        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }
}
```

> **设计亮点**：
> - 使用 Java Record 简化 DTO 定义
> - 统一的 `ApiResponse` 包装响应格式
> - 完善的错误处理和日志记录

---

## 五、API 接口详解

### 5.1 接口汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/vector-store/documents` | 添加单条文档 |
| POST | `/api/vector-store/documents/batch` | 批量添加文档 |
| GET | `/api/vector-store/search` | 相似性搜索 |
| GET | `/api/vector-store/search/threshold` | 带阈值的相似性搜索 |
| GET | `/api/vector-store/search/filter` | 带过滤条件的搜索 |
| DELETE | `/api/vector-store/documents` | 删除所有文档 |
| DELETE | `/api/vector-store/documents/ids` | 根据ID删除文档 |

### 5.2 请求/响应示例

#### 添加文档

**请求**：
```bash
curl -X POST http://localhost:8080/api/vector-store/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring AI 是一个强大的 AI 应用开发框架，它简化了与大型语言模型的集成。",
    "metadata": {
      "category": "technology",
      "author": "admin"
    }
  }'
```

**响应**：
```json
{
  "success": true,
  "message": "Document added successfully",
  "data": null
}
```

#### 相似性搜索

**请求**：
```bash
curl "http://localhost:8080/api/vector-store/search?query=AI框架&topK=3"
```

**响应**：
```json
{
  "success": true,
  "message": "Search completed",
  "data": [
    {
      "id": "doc-001",
      "content": "Spring AI 是一个强大的 AI 应用开发框架...",
      "metadata": {
        "category": "technology",
        "author": "admin"
      }
    }
  ]
}
```

#### 带过滤条件的搜索

**请求**：
```bash
curl "http://localhost:8080/api/vector-store/search/filter?query=AI&filter=category=='technology'&topK=5"
```

> **过滤表达式语法**：支持 `==`、`!=`、`>`、`<`、`>=`、`<=`、`&&`、`||` 等运算符

---

## 六、测试与效果展示

### 6.1 启动应用

```bash
mvn spring-boot:run
```

[建议：此处插入应用启动日志截图，显示 Elasticsearch 连接成功信息]

### 6.2 使用 Postman 或 curl 测试

1. **添加测试数据**：
```bash
# 添加文档1
curl -X POST http://localhost:8080/api/vector-store/documents \
  -H "Content-Type: application/json" \
  -d '{"content": "Spring Boot 简化了 Java 应用的开发和部署", "metadata": {"category": "java", "tag": "spring"}}'

# 添加文档2
curl -X POST http://localhost:8080/api/vector-store/documents \
  -H "Content-Type: application/json" \
  -d '{"content": "Elasticsearch 是一个分布式搜索和分析引擎", "metadata": {"category": "database", "tag": "search"}}'

# 添加文档3
curl -X POST http://localhost:8080/api/vector-store/documents \
  -H "Content-Type: application/json" \
  -d '{"content": "向量数据库在 AI 时代变得越来越重要", "metadata": {"category": "ai", "tag": "vector"}}'
```

2. **执行语义搜索**：
```bash
curl "http://localhost:8080/api/vector-store/search?query=Java开发框架&topK=2"
```

[建议：此处插入搜索结果截图，展示语义匹配效果]

### 6.3 验证 Elasticsearch 索引

访问 Kibana Dev Tools 或使用 curl 查看索引：

```bash
curl http://localhost:9200/spring-ai-document-index/_search?pretty
```

---

## 七、常见问题与解决方案

### 7.1 连接 Elasticsearch 失败

**现象**：启动时报 `Connection refused` 错误

**解决方案**：
1. 检查 Elasticsearch 是否已启动：`docker ps | grep elasticsearch`
2. 确认端口配置正确（默认 9200）
3. 检查防火墙设置

### 7.2 向量维度不匹配

**现象**：添加文档时报 `dimension mismatch` 错误

**解决方案**：
- 检查 `application.yml` 中的 `dimensions` 配置
- OpenAI `text-embedding-ada-002` 输出 1536 维
- 国内模型如 `text-embedding-v1` 输出 1536 维或 768 维

### 7.3 相似度搜索结果为空

**现象**：搜索返回空结果

**解决方案**：
1. 检查是否已添加文档
2. 降低 `similarityThreshold` 阈值（默认 0.0）
3. 检查 Embedding 模型是否正常工作

### 7.4 响应式编程异常

**现象**：出现 `block()/blockFirst()/blockLast()` 错误

**解决方案**：
- 确保所有阻塞操作都在 `Schedulers.boundedElastic()` 上执行
- 不要混用阻塞和响应式代码

---

## 八、总结与扩展

### 8.1 核心要点回顾

通过本教程，我们完成了：
- ✅ Spring AI 与 Elasticsearch 的集成配置
- ✅ 向量存储的增删改查操作
- ✅ 多种搜索模式（基础搜索、阈值搜索、过滤搜索）
- ✅ 响应式编程在 AI 应用中的实践

### 8.2 进阶扩展方向

1. **集成大模型实现 RAG**：
   - 将搜索结果作为上下文传递给 LLM
   - 实现文档问答功能

2. **多模态支持**：
   - 扩展支持图片、音频的向量存储
   - 使用 CLIP 等多模态 Embedding 模型

3. **性能优化**：
   - 添加 Redis 缓存层
   - 实现异步批量导入
   - 配置 Elasticsearch 分片和副本策略

4. **生产环境完善**：
   - 添加认证授权（Spring Security）
   - 实现 API 限流
   - 配置监控和告警（Micrometer + Prometheus）

### 8.3 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Elasticsearch 向量搜索指南](https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html)
- [项目完整代码仓库](https://github.com/your-repo/spring-ai-jc)

---

## 小结

本文详细介绍了如何使用 **Spring AI** 和 **Elasticsearch** 构建向量存储系统。通过实战代码，我们展示了文档的添加、语义搜索、过滤查询等核心功能。

向量数据库是 AI 应用的基石，掌握这项技术将帮助你在 RAG、推荐系统、智能搜索等领域构建更强大的应用。

如果你觉得本文有帮助，欢迎点赞、收藏、转发！有任何问题可以在评论区留言讨论。

---

**💰 为什么选择 32ai？**

**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

---

<!-- 互动提示 -->
欢迎在评论区交流讨论！

**原创声明**：本文为原创教程，转载请注明出处
