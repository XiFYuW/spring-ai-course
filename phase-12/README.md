# Spring AI + Redis 向量存储实战：从零构建智能文档检索系统

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-12](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-12)

## 目录

- [引言](#引言)
- [技术栈介绍](#技术栈介绍)
- [环境准备](#环境准备)
- [项目搭建](#项目搭建)
- [核心代码实现](#核心代码实现)
- [API 接口说明](#api-接口说明)
- [测试验证](#测试验证)
- [常见问题与解决方案](#常见问题与解决方案)
- [总结与扩展](#总结与扩展)

---

## 引言

在 AI 应用开发中，**向量检索（Vector Search）** 是实现语义搜索、知识库问答、推荐系统等场景的核心技术。传统的关键词搜索无法理解文本的语义含义，而向量检索通过将文本转换为高维向量，能够捕捉语义相似性，实现"懂你所想"的智能搜索。

本教程将带你从零开始，使用 **Spring AI + Redis Stack** 构建一个完整的向量文档存储与检索系统。你将学会：

- 如何集成 Spring AI 与 Redis 向量存储
- 如何实现文档的增删改查与语义搜索
- 如何处理生产环境中的常见问题

---

## 技术栈介绍

### 1. Spring AI

Spring AI 是 Spring 官方推出的 AI 应用开发框架，提供了统一的 API 抽象，支持多种 AI 模型（OpenAI、Azure、Ollama 等）和向量存储（Redis、Elasticsearch、PostgreSQL 等）。

**核心优势：**
- 与 Spring Boot 生态无缝集成
- 统一的 `VectorStore` 接口，易于切换底层实现
- 自动配置，开箱即用

### 2. Redis Stack

Redis Stack 是 Redis 的扩展版本，集成了多个模块：
- **Redis Search**：支持全文搜索和向量搜索
- **RedisJSON**：支持 JSON 数据类型
- **RedisTimeSeries**：时序数据存储

**为什么选择 Redis Stack？**
- 高性能：内存存储，毫秒级响应
- 向量搜索：原生支持 HNSW 近似最近邻算法
- 简单易用：无需额外安装搜索引擎

### 3. 项目架构

```
┌─────────────────────────────────────────────────────────┐
│                    客户端 (HTTP)                         │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              VectorStoreController                      │
│         (REST API 接口层)                                │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              VectorStoreService                         │
│         (业务逻辑层)                                     │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              VectorStore (Spring AI)                    │
│         (向量存储抽象接口)                                │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              Redis Stack                                │
│         (向量存储实现)                                   │
└─────────────────────────────────────────────────────────┘
```

---

## 环境准备

### 1. 前置要求

- **JDK 17+**：本项目使用 JDK 25
- **Maven 3.6+**：依赖管理
- **Redis Stack**：向量存储服务

### 2. 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的[https://ai.32zi.com](https://ai.32zi.com)）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3. 安装 Redis Stack

#### 方式一：Docker 安装（推荐）

```bash
# 拉取并启动 Redis Stack
docker run -d --name redis-stack -p 6379:6379 redis/redis-stack:latest

# 验证安装
docker exec -it redis-stack redis-cli FT._LIST
```

#### 方式二：Windows 本地安装

1. 下载 Redis Stack MSI 安装包：https://redis.io/downloads/
2. 运行安装程序，按向导完成安装
3. 启动 Redis Stack 服务：
   ```powershell
   net start redis-stack-server
   ```

#### 方式三：Redis Cloud（云端免费版）

1. 访问 https://redis.io/try-free/ 注册账号
2. 创建免费数据库（默认包含 Redis Search 模块）
3. 获取连接信息（host、port、password）

### 4. 验证 Redis 向量搜索功能

```bash
redis-cli -h 127.0.0.1 -p 6379

# 输入以下命令测试
127.0.0.1:6379> FT._LIST
(empty array)
```

如果返回 `(empty array)` 或 `[]`，说明 Redis Search 模块已正确加载。

**注意**：如果提示 `ERR unknown command 'FT._LIST'`，说明你的 Redis 不支持向量搜索，需要安装 Redis Stack。

---

## 项目搭建

### 1. 创建 Spring Boot 项目

使用 Spring Initializr 创建项目，或直接在 IDE 中创建 Maven 项目。

### 2. 添加依赖

[pom.xml](file:///d:/meta/spring-ai-course/phase-12/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>spring-ai-redis-vector-store</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
    </parent>

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
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring AI OpenAI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        
        <!-- Spring AI Redis Vector Store Starter -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-redis</artifactId>
        </dependency>
    </dependencies>
</project>
```

**关键依赖说明：**
- `spring-ai-starter-vector-store-redis`：Redis 向量存储自动配置
- `spring-ai-starter-model-openai`：OpenAI 嵌入模型（用于文本向量化）

### 3. 配置文件

[application.yml](file:///d:/meta/spring-ai-course/phase-12/src/main/resources/application.yml)

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: https://api.openai.com
      chat:
        options:
          model: text-embedding-ada-002
    vectorstore:
      redis:
        # 是否初始化所需的模式（索引）
        initialize-schema: true
        # 存储向量的索引名称
        index-name: spring-ai-index
        # Redis键的前缀
        prefix: embedding
  data:
    redis:
      # Redis服务器地址
      host: 127.0.0.1
      # Redis服务器端口
      port: 6379
      # Redis密码（如果有）
      password: your-password
      # 连接超时时间
      timeout: 10s
      # 数据库索引
      database: 0

server:
  port: 8080
```

**配置参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `spring.ai.vectorstore.redis.initialize-schema` | 是否自动创建索引 | `false` |
| `spring.ai.vectorstore.redis.index-name` | 向量索引名称 | `spring-ai-index` |
| `spring.ai.vectorstore.redis.prefix` | Redis 键前缀 | `embedding` |
| `spring.data.redis.host` | Redis 服务器地址 | `localhost` |
| `spring.data.redis.port` | Redis 服务器端口 | `6379` |

---

## 核心代码实现

### 1. 启动类

[SpringAiJcStart.java](file:///d:/meta/spring-ai-course/phase-12/src/main/java/org/example/SpringAiJcStart.java)

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

### 2. 服务层：VectorStoreService

[VectorStoreService.java](file:///d:/meta/spring-ai-course/phase-12/src/main/java/org/example/service/VectorStoreService.java)

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
 * 基于 Redis 向量存储实现
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

**代码要点：**
- 使用 `VectorStore` 接口，与具体实现解耦
- 使用 Reactor 的 `Mono` 实现异步非阻塞操作
- `Schedulers.boundedElastic()` 用于执行阻塞的向量存储操作

### 3. 控制器层：VectorStoreController

[VectorStoreController.java](file:///d:/meta/spring-ai-course/phase-12/src/main/java/org/example/controller/VectorStoreController.java)

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
 * 提供文档的增删改查接口，基于 Redis 向量存储
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

---

## API 接口说明

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/vector-store/documents` | 添加单个文档 |
| POST | `/api/vector-store/documents/batch` | 批量添加文档 |
| GET | `/api/vector-store/search` | 相似性搜索 |
| GET | `/api/vector-store/search/threshold` | 带阈值的相似性搜索 |
| GET | `/api/vector-store/search/filter` | 带过滤条件的搜索 |
| DELETE | `/api/vector-store/documents` | 删除所有文档 |
| DELETE | `/api/vector-store/documents/ids` | 根据ID删除文档 |

### 请求示例

#### 1. 添加文档

```bash
curl -X POST http://localhost:8080/api/vector-store/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring AI 是 Spring 框架的 AI 应用开发工具包，提供了统一的 API 抽象。",
    "metadata": {
      "category": "technology",
      "author": "admin"
    }
  }'
```

#### 2. 批量添加文档

```bash
curl -X POST http://localhost:8080/api/vector-store/documents/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"content": "Redis 是一个高性能的键值存储数据库。", "metadata": {"category": "database"}},
    {"content": "向量搜索是 AI 应用的核心技术之一。", "metadata": {"category": "ai"}},
    {"content": "Spring Boot 简化了 Spring 应用的开发。", "metadata": {"category": "framework"}}
  ]'
```

#### 3. 相似性搜索

```bash
curl "http://localhost:8080/api/vector-store/search?query=Spring框架&topK=3"
```

**响应示例：**

```json
{
  "success": true,
  "message": "Search completed",
  "data": [
    {
      "id": "doc-001",
      "content": "Spring Boot 简化了 Spring 应用的开发。",
      "metadata": {
        "category": "framework"
      }
    },
    {
      "id": "doc-002",
      "content": "Spring AI 是 Spring 框架的 AI 应用开发工具包。",
      "metadata": {
        "category": "technology"
      }
    }
  ]
}
```

#### 4. 带过滤条件的搜索

```bash
curl "http://localhost:8080/api/vector-store/search/filter?query=数据库&filter=category=='database'&topK=5"
```

---

## 测试验证

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 验证 Redis 连接

查看应用日志，确认 Redis 向量存储初始化成功：

```
INFO  o.s.a.v.r.RedisVectorStore : Created index spring-ai-index for vector store
```

### 3. 功能测试

使用 Postman 或 curl 测试上述 API 接口。

---

## 常见问题与解决方案

### 问题1：`ERR unknown command 'FT._LIST'`

**原因**：Redis 服务器不支持向量搜索功能（缺少 Redis Search 模块）。

**解决**：安装 Redis Stack 而非普通 Redis。

```bash
# Docker 方式
docker run -d --name redis-stack -p 6379:6379 redis/redis-stack:latest
```

### 问题2：`Connection refused: no further information`

**原因**：Redis 服务未启动或配置错误。

**解决**：
1. 检查 Redis 服务状态
2. 验证 `application.yml` 中的 host 和 port 配置

### 问题3：`Failed to initialize schema`

**原因**：
- `initialize-schema` 设置为 `false`，但索引不存在
- Redis 用户权限不足

**解决**：
```yaml
spring:
  ai:
    vectorstore:
      redis:
        initialize-schema: true  # 确保为 true
```

### 问题4：搜索结果为空

**原因**：
- 文档未成功添加
- 嵌入模型配置错误
- 查询文本与文档语义差异过大

**解决**：
1. 检查 API 调用是否返回成功
2. 验证 OpenAI API Key 和 base-url 配置
3. 使用更相关的查询文本测试

---

## 总结与扩展

### 本教程核心要点

1. **Spring AI 的抽象设计**：通过 `VectorStore` 接口，可以轻松切换底层向量存储实现（Redis、Elasticsearch、PostgreSQL 等）。

2. **Redis Stack 的优势**：
   - 内存存储，查询性能极高
   - 原生支持向量搜索（HNSW 算法）
   - 与 Spring Boot 生态无缝集成

3. **响应式编程**：使用 Reactor 的 `Mono` 实现异步非阻塞操作，提升系统吞吐量。

### 扩展方向

1. **多模态向量存储**：不仅存储文本，还可以存储图片、音频的向量表示。

2. **混合搜索**：结合关键词搜索（BM25）和向量搜索，提升搜索准确性。

3. **知识库问答（RAG）**：
   - 将用户问题向量化
   - 在向量存储中检索相关文档
   - 将检索结果作为上下文，调用大语言模型生成回答

4. **性能优化**：
   - 使用 Redis 集群提升存储容量和查询性能
   - 调整 HNSW 算法参数（`M`、`EF_CONSTRUCTION`、`EF_RUNTIME`）平衡精度和速度

5. **监控与运维**：
   - 集成 Spring Boot Actuator 监控 Redis 连接状态
   - 使用 RedisInsight 可视化查看向量数据

---

## 参考资源

- [Spring AI 官方文档 - Redis Vector Store](https://docs.springframework.org.cn/spring-ai/reference/api/vectordbs/redis.html)
- [Redis Stack 官方文档](https://redis.io/docs/stack/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)

---


**原创声明**：本文为原创教程，转载请注明出处。

欢迎在评论区交流讨论！
