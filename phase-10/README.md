# Spring AI MCP 实战：将你的服务升级为 AI 可调用的智能工具

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-10](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-10)

## 引言

在 AI 大模型蓬勃发展的今天，如何让 AI 能够安全、标准化地调用我们的业务系统？**MCP（Model Context Protocol）** 协议应运而生！它就像 AI 世界的 "USB 接口"，让你的服务可以被任何支持 MCP 的 AI 客户端调用。

**本文将带你**：
- 理解 MCP 协议的核心概念与架构
- 手把手实现一个商品管理的 MCP 服务
- 掌握举一反三的能力，将任意业务系统改造为 MCP 服务
- 学会如何作为第三方 MCP 提供商对外提供服务

**读完本文，你将能够**：
- 将现有的商品服务、订单服务、用户服务等改造为 MCP 服务
- 理解 MCP Server 和 MCP Client 的协作机制
- 掌握 Spring AI MCP 的核心注解和配置

---

## 目录

- [一、MCP 协议核心概念](#一mcp-协议核心概念)
- [二、项目架构与模块说明](#二项目架构与模块说明)
- [三、环境准备](#三环境准备)
- [四、MCP Server 开发实战](#四mcp-server-开发实战)
- [五、MCP Client 开发实战](#五mcp-client-开发实战)
- [六、举一反三：改造你的业务系统](#六举一反三改造你的业务系统)
- [七、常见问题与避坑指南](#七常见问题与避坑指南)
- [八、总结与扩展思考](#八总结与扩展思考)

---

## 一、MCP 协议核心概念

### 1.1 什么是 MCP？

**MCP（Model Context Protocol）** 是由 Anthropic 推出的开放协议，旨在标准化 AI 模型与外部工具、数据源之间的交互方式。

**核心思想**：
```
┌─────────────────┐         MCP 协议          ┌─────────────────┐
│   AI 大模型      │  ◄────────────────────►  │   你的业务系统    │
│  (Claude/GPT)   │    标准化工具调用接口      │  (商品/订单/用户) │
└─────────────────┘                          └─────────────────┘
         │                                           │
         │         ┌─────────────────┐              │
         └────────►│   MCP Client    │─────────────►│
                   │  (客户端代理)    │              │
                   └─────────────────┘              │
                            │                      │
                            ▼                      ▼
                   ┌─────────────────┐    ┌─────────────────┐
                   │   MCP Server    │◄───│   业务数据库     │
                   │  (工具提供者)    │    │  (PostgreSQL)   │
                   └─────────────────┘    └─────────────────┘
```

### 1.2 MCP 的核心组件

| 组件 | 作用 | 类比 |
|------|------|------|
| **MCP Server** | 暴露工具（Tools）给 AI 调用 | 就像提供 API 接口的后端服务 |
| **MCP Client** | 连接 Server，代理 AI 的调用请求 | 就像 API 调用方/SDK |
| **Tools** | 具体的功能实现（如查询商品、创建订单） | 就像 REST API 的端点 |
| **Resources** | 可被 AI 读取的数据资源 | 就像静态文件或数据快照 |
| **Prompts** | 预定义的提示词模板 | 就像预设的指令模板 |

### 1.3 为什么选择 Spring AI MCP？

Spring AI MCP 提供了：
- **声明式注解**：用 `@McpTool` 轻松暴露方法为 AI 可调用的工具
- **响应式编程**：基于 WebFlux + R2DBC，高并发场景性能优异
- **标准化协议**：符合 MCP 规范，兼容 Claude Desktop、Cursor 等客户端
- **灵活部署**：可作为独立服务，也可嵌入现有 Spring Boot 应用

---

## 二、项目架构与模块说明

本项目采用 Maven 多模块架构：

```
spring-ai-mcp-demo/
├── pom.xml                    # 父 POM，统一管理依赖版本
├── mcp-server/                # MCP 服务端 - 提供商品管理工具
│   ├── src/main/java/
│   │   └── org/example/server/
│   │       ├── entity/        # 实体类：Product
│   │       ├── repository/    # 数据访问层：ProductRepository
│   │       ├── tool/          # MCP 工具类：ProductTools ⭐核心
│   │       └── McpServerApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml    # 服务端配置
│   │   └── schema.sql         # 数据库初始化脚本
│   └── pom.xml
└── mcp-client/                # MCP 客户端 - 调用服务端工具
    ├── src/main/java/
    │   └── org/example/client/
    │       ├── controller/    # REST API：ProductController
    │       ├── service/       # 服务层：ProductMcpService ⭐核心
    │       └── McpClientApplication.java
    ├── src/main/resources/
    │   └── application.yml    # 客户端配置
    └── pom.xml
```

**[建议：此处插入项目结构截图，展示 IDEA 的项目视图]**

---

## 三、环境准备

### 3.1 技术栈要求

- **JDK 25+**（本项目使用 JDK 25）
- **Spring Boot 3.5.10+**
- **Spring AI 1.1.0-SNAPSHOT**
- **PostgreSQL 14+**（用于数据存储）
- **Maven 3.8+**

### 3.2 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3.3 创建数据库

```sql
-- 创建数据库
CREATE DATABASE chatdb;

-- 创建商品表
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10, 2),
    stock INTEGER DEFAULT 0,
    category VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
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

    <!-- Spring 里程碑仓库配置 -->
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
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring AI BOM -->
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

## 四、MCP Server 开发实战

MCP Server 是整个架构的核心，它负责将业务功能暴露为 AI 可调用的工具。

### 4.1 添加依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- MCP 服务器 WebFlux 启动器 ⭐核心依赖 -->
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

### 4.2 配置 application.yml

```yaml
spring:
  application:
    name: mcp-product-server
  ai:
    mcp:
      server:
        protocol: STREAMABLE  # 可流式传输的 HTTP MCP 服务
        enabled: true
        name: webflux-mcp-server
        version: 1.0.0
        type: ASYNC           # 异步模式
        instructions: "This reactive server provides product management tools"
        annotation-scanner:
          enabled: true       # 启用注解扫描
        capabilities:
          tool: true          # 启用工具能力
          resource: true
          prompt: true
        streamable-http:
          mcp-endpoint: /api/mcp    # MCP 端点路径
          keep-alive-interval: 30s

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

### 4.3 创建实体类

```java
package org.example.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Table("products")
public class Product {

    @Id
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String description, Double price, 
                   Integer stock, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters...
}
```

### 4.4 创建 Repository 接口

```java
package org.example.server.repository;

import org.example.server.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Mono<Product> findByName(String name);

    Flux<Product> findByCategory(String category);

    Flux<Product> findByStatus(String status);

    @Query("SELECT * FROM products WHERE price >= :minPrice AND price <= :maxPrice")
    Flux<Product> findByPriceRange(Double minPrice, Double maxPrice);

    @Query("SELECT * FROM products WHERE name LIKE '%' || :keyword || '%'")
    Flux<Product> findByNameContaining(String keyword);

    @Query("SELECT COUNT(*) FROM products")
    Mono<Long> countAll();
}
```

### 4.5 核心：创建 MCP 工具类 ⭐

这是最关键的部分！使用 `@McpTool` 注解将业务方法暴露为 AI 可调用的工具：

```java
package org.example.server.tool;

import org.example.server.entity.Product;
import org.example.server.repository.ProductRepository;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.format.DateTimeFormatter;

/**
 * MCP 服务器 - 商品数据库操作工具提供者
 * 使用 Spring AI MCP 注解暴露商品增删改查功能
 */
@Component
public class ProductTools {

    private final ProductRepository productRepository;

    public ProductTools(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 创建新商品
     * @McpTool 注解将此方法暴露为 AI 可调用的工具
     */
    @McpTool(
            name = "createProduct",  // 工具名称，AI 会通过这个名字调用
            description = "创建新商品，需要商品名称、描述、价格、库存和分类"
    )
    public Mono<String> createProduct(
            // @McpToolParam 定义参数说明，帮助 AI 理解如何传参
            @McpToolParam(description = "商品名称，必填", required = true) String name,
            @McpToolParam(description = "商品描述", required = false) String description,
            @McpToolParam(description = "商品价格，必填", required = true) Double price,
            @McpToolParam(description = "商品库存", required = false) Integer stock,
            @McpToolParam(description = "商品分类", required = false) String category) {

        return productRepository.findByName(name)
                .flatMap(existingProduct -> {
                    String errorMsg = "❌ 创建失败：商品 '" + name + "' 已存在";
                    return Mono.just(errorMsg);
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            Product newProduct = new Product(name, description, price, stock, category);
                            return productRepository.save(newProduct)
                                    .map(savedProduct -> 
                                        "✅ 商品创建成功！\n" + formatProduct(savedProduct));
                        })
                );
    }

    /**
     * 根据ID查询商品
     */
    @McpTool(
            name = "getProductById",
            description = "根据商品ID查询商品信息"
    )
    public Mono<String> getProductById(
            @McpToolParam(description = "商品ID，必填", required = true) Long id) {

        return productRepository.findById(id)
                .map(product -> "✅ 查询成功！\n" + formatProduct(product))
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品");
    }

    /**
     * 根据商品名称查询
     */
    @McpTool(
            name = "getProductByName",
            description = "根据商品名称查询商品信息"
    )
    public Mono<String> getProductByName(
            @McpToolParam(description = "商品名称，必填", required = true) String name) {

        return productRepository.findByName(name)
                .map(product -> "✅ 查询成功！\n" + formatProduct(product))
                .defaultIfEmpty("❌ 未找到商品名称为 '" + name + "' 的商品");
    }

    /**
     * 查询所有商品
     */
    @McpTool(
            name = "getAllProducts",
            description = "查询所有商品列表"
    )
    public Mono<String> getAllProducts() {
        return productRepository.findAll()
                .collectList()
                .flatMap(products -> {
                    if (products.isEmpty()) {
                        return Mono.just("📭 暂无商品数据");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 商品列表（共 ").append(products.size()).append(" 条）：\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 更新商品
     */
    @McpTool(
            name = "updateProduct",
            description = "根据商品ID更新商品信息"
    )
    public Mono<String> updateProduct(
            @McpToolParam(description = "商品ID，必填", required = true) Long id,
            @McpToolParam(description = "新商品名称（不修改传null）", required = false) String name,
            @McpToolParam(description = "新描述（不修改传null）", required = false) String description,
            @McpToolParam(description = "新价格（不修改传null）", required = false) Double price,
            @McpToolParam(description = "新库存（不修改传null）", required = false) Integer stock,
            @McpToolParam(description = "新分类（不修改传null）", required = false) String category,
            @McpToolParam(description = "新状态（不修改传null）", required = false) String status) {

        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    // 只更新非空字段
                    if (name != null && !name.isEmpty()) existingProduct.setName(name);
                    if (description != null) existingProduct.setDescription(description);
                    if (price != null) existingProduct.setPrice(price);
                    if (stock != null) existingProduct.setStock(stock);
                    if (category != null) existingProduct.setCategory(category);
                    if (status != null) existingProduct.setStatus(status);
                    existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

                    return productRepository.save(existingProduct)
                            .map(updatedProduct -> 
                                "✅ 商品更新成功！\n" + formatProduct(updatedProduct));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品");
    }

    /**
     * 删除商品
     */
    @McpTool(
            name = "deleteProduct",
            description = "根据商品ID删除商品"
    )
    public Mono<String> deleteProduct(
            @McpToolParam(description = "商品ID，必填", required = true) Long id) {

        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    String productName = existingProduct.getName();
                    return productRepository.deleteById(id)
                            .then(Mono.just("✅ 商品删除成功！已删除：" + productName));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品");
    }

    /**
     * 统计商品总数
     */
    @McpTool(
            name = "countProducts",
            description = "统计系统中的商品总数"
    )
    public Mono<String> countProducts() {
        return productRepository.countAll()
                .map(count -> "📊 系统商品总数：" + count + " 件");
    }

    /**
     * 格式化商品信息
     */
    private String formatProduct(Product product) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "📦 商品ID: %d\n" +
                "   商品名称: %s\n" +
                "   描述: %s\n" +
                "   价格: ¥%.2f\n" +
                "   库存: %d\n" +
                "   分类: %s\n" +
                "   状态: %s\n" +
                "   创建时间: %s",
                product.getId(),
                product.getName(),
                product.getDescription() != null ? product.getDescription() : "暂无描述",
                product.getPrice() != null ? product.getPrice() : 0.0,
                product.getStock() != null ? product.getStock() : 0,
                product.getCategory() != null ? product.getCategory() : "未分类",
                product.getStatus(),
                product.getCreatedAt() != null ? product.getCreatedAt().format(formatter) : "未知"
        );
    }
}
```

### 4.6 启动类

```java
package org.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("MCP 服务器已启动！端口: 8080");
    }
}
```

**[建议：此处插入 MCP Server 启动成功的控制台截图]**

---

## 五、MCP Client 开发实战

MCP Client 负责连接到 MCP Server，并将工具调用代理给 AI 模型。

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

    <!-- MCP 客户端 WebFlux 启动器 ⭐核心依赖 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
    </dependency>

    <!-- Spring AI OpenAI（用于 AI 对话） -->
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
    name: mcp-product-client
  ai:
    openai:
      api-key: your-api-key
      base-url: https://ai.32zi.com  # 可替换为你的 API 代理地址
      chat:
        options:
          model: claude-3-7-sonnet-latest
    
    # MCP 客户端配置
    mcp:
      client:
        name: product-server
        version: 1.0.0
        enabled: true
        type: ASYNC
        request-timeout: 30s
        streamable-http:
          connections:
            product-server:           # 连接名称
              url: http://localhost:8080   # MCP Server 地址
              endpoint: /api/mcp           # MCP 端点

server:
  port: 8081
```

### 5.3 创建 Service 层

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

@Service
public class ProductMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public ProductMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        // 从客户端列表中找到 product-server
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> client.getClientInfo().name()
                        .equals("product-server - product-server"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 product-server 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        // 初始化时打印可用工具列表
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
     * 创建商品 - 调用 MCP Server 的 createProduct 工具
     */
    public Mono<String> createProduct(String name, String description, 
                                       Double price, Integer stock, String category) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("name", name);
        if (description != null) params.put("description", description);
        if (price != null) params.put("price", price);
        if (stock != null) params.put("stock", stock);
        if (category != null) params.put("category", category);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("createProduct", params)
        ).map(this::extractResult);
    }

    /**
     * 查询商品 - 调用 MCP Server 的 getProductById 工具
     */
    public Mono<String> getProductById(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductById",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * 查询所有商品
     */
    public Mono<String> getAllProducts() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("getAllProducts", Map.of())
        ).map(this::extractResult);
    }

    /**
     * 更新商品
     */
    public Mono<String> updateProduct(Long id, String name, String description, 
                                       Double price, Integer stock, String category, String status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        if (name != null) params.put("name", name);
        if (description != null) params.put("description", description);
        if (price != null) params.put("price", price);
        if (stock != null) params.put("stock", stock);
        if (category != null) params.put("category", category);
        if (status != null) params.put("status", status);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("updateProduct", params)
        ).map(this::extractResult);
    }

    /**
     * 删除商品
     */
    public Mono<String> deleteProduct(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "deleteProduct",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * AI 智能问答（流式输出）
     */
    public Flux<String> askProductAIStream(String question) {
        return Mono.zip(
                getAllProducts().defaultIfEmpty("暂无商品数据"),
                countProducts().defaultIfEmpty("0")
        ).flatMapMany(tuple -> {
            String products = tuple.getT1();
            String count = tuple.getT2();

            String prompt = String.format(
                    "你是商品管理系统助手。基于以下数据回答用户问题。\n\n" +
                    "商品统计：%s\n\n商品列表：\n%s\n\n用户问题：%s",
                    count, products, question
            );

            return chatClientBuilder.build()
                    .prompt(prompt)
                    .stream()
                    .content();
        });
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

### 5.4 创建 REST Controller

```java
package org.example.client.controller;

import org.example.client.service.ProductMcpService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductMcpService productMcpService;

    public ProductController(ProductMcpService productMcpService) {
        this.productMcpService = productMcpService;
    }

    /**
     * 创建商品
     * POST /api/products
     */
    @PostMapping
    public Mono<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Double price = request.get("price") != null ? 
            Double.valueOf(request.get("price").toString()) : null;
        Integer stock = request.get("stock") != null ? 
            Integer.valueOf(request.get("stock").toString()) : null;
        String category = (String) request.get("category");

        return productMcpService.createProduct(name, description, price, stock, category)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 根据ID查询商品
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getProductById(@PathVariable Long id) {
        return productMcpService.getProductById(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "data", result
                ));
    }

    /**
     * 查询所有商品
     * GET /api/products
     */
    @GetMapping
    public Mono<Map<String, Object>> getAllProducts() {
        return productMcpService.getAllProducts()
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "data", result
                ));
    }

    /**
     * 更新商品
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        // 参数处理...
        return productMcpService.updateProduct(id, /* 参数 */)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 删除商品
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        return productMcpService.deleteProduct(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * AI 智能商品问答（流式输出）
     * POST /api/products/ask/stream
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askProductAIStream(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "请介绍一下当前商品情况");
        return productMcpService.askProductAIStream(question);
    }
}
```

**[建议：此处插入 API 测试截图，如 Postman 或浏览器访问结果]**

---

## 六、举一反三：改造你的业务系统

现在你已经掌握了商品管理的 MCP 服务实现，接下来学习如何**举一反三**，将任意业务系统改造为 MCP 服务。

### 6.1 改造公式

```
任意业务系统 → MCP 服务的三步转换法：

┌─────────────────────────────────────────────────────────────┐
│  第一步：识别业务操作                                          │
│  ├── 列出系统中的核心功能（增删改查、审批、统计等）               │
│  └── 确定哪些操作适合暴露给 AI（查询类优先，敏感操作需谨慎）       │
├─────────────────────────────────────────────────────────────┤
│  第二步：创建 MCP 工具类                                       │
│  ├── 新建 XxxTools.java 类，添加 @Component 注解               │
│  ├── 为每个业务操作添加 @McpTool 注解                          │
│  ├── 使用 @McpToolParam 描述参数                               │
│  └── 返回 String 类型，便于 AI 理解和展示                      │
├─────────────────────────────────────────────────────────────┤
│  第三步：配置与启动                                            │
│  ├── 添加 spring-ai-starter-mcp-server-webflux 依赖            │
│  ├── 配置 application.yml 中的 mcp.server 参数                 │
│  └── 启动服务，验证工具是否被正确暴露                           │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 实战案例：订单服务改造

假设你有一个订单服务，如何改造为 MCP 服务？

```java
@Component
public class OrderTools {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderTools(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * 查询订单详情
     */
    @McpTool(
            name = "getOrderById",
            description = "根据订单ID查询订单详情，包括商品信息、金额、状态等"
    )
    public Mono<String> getOrderById(
            @McpToolParam(description = "订单ID，必填", required = true) String orderId) {
        
        return orderRepository.findById(orderId)
                .map(this::formatOrder)
                .defaultIfEmpty("❌ 未找到订单：" + orderId);
    }

    /**
     * 查询用户订单列表
     */
    @McpTool(
            name = "getUserOrders",
            description = "查询指定用户的所有订单"
    )
    public Mono<String> getUserOrders(
            @McpToolParam(description = "用户ID，必填", required = true) Long userId,
            @McpToolParam(description = "订单状态筛选：PENDING/PAID/SHIPPED/COMPLETED", required = false) String status) {
        
        Flux<Order> orders = status != null 
                ? orderRepository.findByUserIdAndStatus(userId, status)
                : orderRepository.findByUserId(userId);
        
        return orders.collectList()
                .map(list -> {
                    if (list.isEmpty()) return "📭 该用户暂无订单";
                    StringBuilder sb = new StringBuilder("📋 订单列表（共 " + list.size() + " 条）：\n");
                    list.forEach(order -> sb.append(formatOrder(order)).append("\n"));
                    return sb.toString();
                });
    }

    /**
     * 创建订单
     */
    @McpTool(
            name = "createOrder",
            description = "创建新订单，需要提供用户ID、商品ID列表和收货地址"
    )
    public Mono<String> createOrder(
            @McpToolParam(description = "用户ID，必填", required = true) Long userId,
            @McpToolParam(description = "商品ID列表，多个用逗号分隔，如：1,2,3", required = true) String productIds,
            @McpToolParam(description = "收货地址，必填", required = true) String address) {
        
        return orderService.createOrder(userId, productIds, address)
                .map(order -> "✅ 订单创建成功！\n" + formatOrder(order))
                .onErrorResume(e -> Mono.just("❌ 创建失败：" + e.getMessage()));
    }

    /**
     * 取消订单
     */
    @McpTool(
            name = "cancelOrder",
            description = "取消未发货的订单"
    )
    public Mono<String> cancelOrder(
            @McpToolParam(description = "订单ID，必填", required = true) String orderId,
            @McpToolParam(description = "取消原因", required = false) String reason) {
        
        return orderService.cancelOrder(orderId, reason)
                .map(success -> success 
                        ? "✅ 订单 " + orderId + " 已取消" 
                        : "❌ 取消失败，订单可能已发货")
                .onErrorResume(e -> Mono.just("❌ 取消失败：" + e.getMessage()));
    }

    /**
     * 统计订单数据
     */
    @McpTool(
            name = "getOrderStatistics",
            description = "获取订单统计数据，包括总订单数、总金额、各状态订单数"
    )
    public Mono<String> getOrderStatistics() {
        
        return orderRepository.getStatistics()
                .map(stats -> String.format(
                        "📊 订单统计\n" +
                        "━━━━━━━━━━━━━━━━\n" +
                        "总订单数: %d\n" +
                        "总金额: ¥%.2f\n" +
                        "待付款: %d\n" +
                        "已付款: %d\n" +
                        "已发货: %d\n" +
                        "已完成: %d",
                        stats.getTotalCount(),
                        stats.getTotalAmount(),
                        stats.getPendingCount(),
                        stats.getPaidCount(),
                        stats.getShippedCount(),
                        stats.getCompletedCount()
                ));
    }

    private String formatOrder(Order order) {
        return String.format(
                "📦 订单号: %s\n" +
                "   用户ID: %d\n" +
                "   金额: ¥%.2f\n" +
                "   状态: %s\n" +
                "   创建时间: %s",
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
```

### 6.3 更多业务场景示例

| 业务场景 | 可暴露的 MCP 工具 | 适用场景 |
|---------|------------------|---------|
| **用户服务** | `getUserById`, `searchUsers`, `getUserStatistics` | AI 客服、用户分析 |
| **库存服务** | `getStock`, `checkAvailability`, `getLowStockAlerts` | 智能补货、库存预警 |
| **日志服务** | `searchLogs`, `getErrorStats`, `analyzePatterns` | 智能运维、故障排查 |
| **报表服务** | `generateReport`, `getDashboardData`, `exportData` | 智能报表、数据分析 |
| **通知服务** | `sendNotification`, `getNotificationHistory` | 智能提醒、消息推送 |

---

## 七、常见问题与避坑指南

### 7.1 依赖版本冲突

**问题**：Spring AI MCP 需要特定的 Spring Boot 版本

**解决方案**：
```xml
<!-- 确保使用兼容的版本 -->
<properties>
    <spring-boot.version>3.5.10</spring-boot.version>
    <spring-ai.version>1.1.0-SNAPSHOT</spring-ai.version>
</properties>
```

### 7.2 客户端连接失败

**问题**：MCP Client 无法连接到 Server

**排查步骤**：
1. 检查 Server 是否已启动：`curl http://localhost:8080/api/mcp`
2. 检查防火墙是否放行端口
3. 检查 Client 配置中的 `url` 和 `endpoint` 是否正确
4. 查看日志中的连接错误信息

### 7.3 工具调用返回空结果

**问题**：AI 调用工具后没有返回预期结果

**解决方案**：
- 确保 `@McpTool` 的 `name` 和 `description` 清晰明确
- 检查 `@McpToolParam` 的 `description` 是否帮助 AI 理解参数含义
- 在工具方法中添加日志，确认是否被调用

### 7.4 响应式编程陷阱

**问题**：R2DBC 查询没有返回数据

**常见错误**：
```java
// ❌ 错误：阻塞了响应式流
Product product = productRepository.findById(id).block();

// ✅ 正确：保持响应式链
return productRepository.findById(id)
        .map(product -> /* 处理 */);
```

---

## 八、总结与扩展思考

### 8.1 核心知识点回顾

1. **MCP 协议**：标准化 AI 与外部系统的交互方式
2. **@McpTool**：将业务方法暴露为 AI 可调用的工具
3. **@McpToolParam**：描述参数，帮助 AI 正确传参
4. **McpAsyncClient**：客户端代理，转发 AI 的工具调用请求

### 8.2 作为第三方 MCP 提供商的商业模式

如果你有自己的商品服务、订单服务或其他 SaaS 服务，可以：

1. **开发 MCP Server**：将核心功能封装为 MCP 工具
2. **提供连接配置**：向客户提供 `application.yml` 配置模板
3. **文档化工具列表**：详细说明每个工具的名称、参数、返回值
4. **部署与运维**：作为独立服务或嵌入现有系统

**示例：向客户提供的服务配置**

```yaml
# 客户在自己的 MCP Client 中添加以下配置
spring:
  ai:
    mcp:
      client:
        streamable-http:
          connections:
            your-product-service:  # 你的服务名称
              url: https://api.yourcompany.com
              endpoint: /mcp
              headers:
                X-API-Key: ${YOUR_API_KEY}  # 客户的 API Key
```

### 8.3 扩展思考

1. **安全性增强**：
   - 添加 API Key 认证
   - 实现请求限流
   - 敏感操作添加二次确认

2. **性能优化**：
   - 添加 Redis 缓存
   - 实现工具调用结果缓存
   - 使用连接池优化数据库访问

3. **功能扩展**：
   - 支持 Resources（静态资源访问）
   - 支持 Prompts（预定义提示词）
   - 实现 Streaming 流式响应

---

## 附录

### 参考资料

1. [Spring AI MCP 官方文档](https://docs.spring.io/spring-ai/reference/api/mcp.html)
2. [MCP 协议规范](https://modelcontextprotocol.io/)
3. [Anthropic MCP 介绍](https://www.anthropic.com/news/model-context-protocol)


---

**原创声明**：本文为原创教程，转载请注明出处。

欢迎在评论区交流讨论！
