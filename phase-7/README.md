# Spring AI MCP 之 SSE WebFlux 实战：从零构建 AI 天气助手

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-7](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-7)

## 引言

在 AI 应用开发中，如何让大语言模型（LLM）与外部工具无缝集成一直是个难题。MCP（Model Context Protocol）协议的出现，为这个问题提供了优雅的解决方案。本文将带你从零开始，使用 **Spring AI 1.1.0** 和 **MCP 协议**，构建一个完整的 AI 天气助手系统。

通过本文，你将学会：
- MCP 协议的核心概念和工作原理
- Spring AI MCP 的服务端和客户端开发
- 异步响应式编程在 AI 应用中的实践
- 流式输出提升用户体验的技巧

## 目录

- [一、MCP 协议简介](#一mcp-协议简介)
- [二、项目架构设计](#二项目架构设计)
- [三、环境准备](#三环境准备)
- [四、MCP 服务端开发](#四mcp-服务端开发)
- [五、MCP 客户端开发](#五mcp-客户端开发)
- [六、流式输出优化](#六流式输出优化)
- [七、测试与验证](#七测试与验证)
- [八、常见问题与解决方案](#八常见问题与解决方案)
- [九、总结与扩展](#九总结与扩展)

---

## 一、MCP 协议简介

### 1.1 什么是 MCP？

MCP（Model Context Protocol）是一个**开放协议**，它标准化了 AI 模型与外部数据源、工具之间的集成方式。你可以把它理解为 AI 世界的 **"Type-C 接口"** —— 统一的连接标准，让不同的 AI 应用和工具能够即插即用。

### 1.2 MCP 的核心组件

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   MCP Host      │────▶│   MCP Client    │────▶│   MCP Server    │
│  (AI 应用)       │     │  (客户端代理)    │     │  (工具提供者)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │                         │
                              │    JSON-RPC over SSE    │
                              │◄───────────────────────▶│
```

- **MCP Host**：承载 AI 模型的应用程序（如 Claude Desktop、IDE 插件）
- **MCP Client**：与服务器建立连接，管理协议协商
- **MCP Server**：暴露工具、资源和提示，供 AI 模型调用

### 1.3 为什么选择 Spring AI MCP？

Spring AI 从 1.1.0 版本开始提供对 MCP 的原生支持，优势包括：

- **注解驱动开发**：使用 `@McpTool`、`@McpResource` 等注解快速暴露功能
- **自动配置**：Spring Boot 自动装配，减少样板代码
- **响应式支持**：基于 WebFlux 的异步非阻塞实现
- **传输灵活**：支持 STDIO、SSE、Streamable HTTP 等多种传输方式

---

## 二、项目架构设计

### 2.1 系统架构

我们的 AI 天气助手采用 **客户端-服务端分离** 架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户请求                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│  MCP Client (Port: 8081)                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ REST API     │  │ WeatherMcp   │  │ ChatClient (OpenAI)  │  │
│  │ Controller   │──▶│ Service      │──▶│                      │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ SSE Protocol
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│  MCP Server (Port: 8080)                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  WeatherTools (MCP Tools)                                 │  │
│  │  ├── getCurrentWeather()  获取当前天气                    │  │
│  │  ├── getWeatherForecast() 获取天气预报                    │  │
│  │  ├── getAirQuality()      获取空气质量                    │  │
│  │  └── getLifeIndex()       获取生活指数                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

```
spring-ai-mcp-demo/
├── pom.xml                    # 父 POM，统一管理依赖
├── mcp-server/                # MCP 服务端模块
│   ├── src/main/java/
│   │   └── org/example/server/
│   │       ├── McpServerApplication.java
│   │       └── tool/WeatherTools.java
│   └── pom.xml
└── mcp-client/                # MCP 客户端模块
    ├── src/main/java/
    │   └── org/example/client/
    │       ├── McpClientApplication.java
    │       ├── service/WeatherMcpService.java
    │       └── controller/WeatherController.java
    └── pom.xml
```

---

## 三、环境准备

### 3.1 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）[点击访问](https://ai.32zi.com)

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3.2 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 25 | Java 开发套件 |
| Spring Boot | 3.5.10 | 应用框架 |
| Spring AI | 1.1.0-SNAPSHOT | AI 开发框架 |
| Spring AI MCP | 1.1.0-SNAPSHOT | MCP 协议支持 |

### 3.3 创建父项目

**pom.xml**

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

    <!-- Spring 仓库配置 -->
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

    <modules>
        <module>mcp-server</module>
        <module>mcp-client</module>
    </modules>
</project>
```

**关键配置说明**：
- `spring-milestones` 和 `spring-snapshots` 仓库用于获取 Spring AI 的里程碑和快照版本
- `spring-ai-bom` 统一管理 Spring AI 相关依赖版本

---

## 四、MCP 服务端开发

### 4.1 服务端依赖

**mcp-server/pom.xml**

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
</dependencies>

<build>
    <plugins>
        <!-- 关键：启用参数名编译，解决 @RequestParam 等问题 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <parameters>true</parameters>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

### 4.2 服务端配置

**application.yml**

```yaml
spring:
  application:
    name: mcp-weather-server
  ai:
    mcp:
      server:
        enabled: true
        name: webflux-mcp-server      # 服务器名称
        version: 1.0.0                # 服务器版本
        type: ASYNC                   # 异步服务器类型
        instructions: "提供天气信息查询服务"
        annotation-scanner:
          enabled: true               # 启用注解扫描
        capabilities:
          tool: true                  # 启用工具功能
          resource: true
          prompt: true
        sse-message-endpoint: /mcp/messages
        keep-alive-interval: 30s

server:
  port: 8080

logging:
  level:
    io.modelcontextprotocol: DEBUG
```

**重要提示**：`type: ASYNC` 表示使用异步服务器，只注册返回 `Mono<T>` 或 `Flux<T>` 的方法。

### 4.3 天气工具实现

**WeatherTools.java**

```java
package org.example.server.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * MCP 天气工具 - 异步版本
 * 使用 @McpTool 注解暴露为 MCP 工具
 */
@Component
public class WeatherTools {

    private final Random random = new Random();

    @McpTool(
            name = "getCurrentWeather",
            description = "获取指定城市的当前天气信息"
    )
    public Mono<String> getCurrentWeather(
            @McpToolParam(description = "城市名称", required = true) String city) {
        
        String[] weathers = {"晴天", "多云", "阴天", "小雨", "中雨"};
        String weather = weathers[random.nextInt(weathers.length)];
        int temperature = 15 + random.nextInt(20);
        
        return Mono.just(String.format(
            "【%s】当前天气：%s，温度：%d°C，更新时间：%s",
            city, weather, temperature,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));
    }

    @McpTool(
            name = "getWeatherForecast",
            description = "获取未来几天天气预报"
    )
    public Mono<String> getWeatherForecast(
            @McpToolParam(description = "城市名称", required = true) String city,
            @McpToolParam(description = "预报天数", required = true) Integer days) {
        
        StringBuilder forecast = new StringBuilder();
        forecast.append(String.format("【%s】未来%d天天气预报：\n", city, days));
        
        for (int i = 0; i < Math.min(days, 7); i++) {
            forecast.append(String.format("  第%d天：晴天，20°C ~ 28°C\n", i + 1));
        }
        
        return Mono.just(forecast.toString());
    }

    @McpTool(
            name = "getAirQuality",
            description = "获取空气质量指数"
    )
    public Mono<String> getAirQuality(
            @McpToolParam(description = "城市名称", required = true) String city) {
        
        int aqi = 30 + random.nextInt(100);
        String level = aqi <= 50 ? "优" : aqi <= 100 ? "良" : "轻度污染";
        
        return Mono.just(String.format(
            "【%s】空气质量指数(AQI)：%d，等级：%s",
            city, aqi, level
        ));
    }

    @McpTool(
            name = "getLifeIndex",
            description = "获取生活指数建议"
    )
    public Mono<String> getLifeIndex(
            @McpToolParam(description = "城市名称", required = true) String city) {
        
        return Mono.just(String.format(
            "【%s】生活指数：\n  运动指数：适宜\n  洗车指数：较适宜\n  穿衣指数：舒适",
            city
        ));
    }
}
```

**核心注解说明**：

| 注解 | 作用 | 参数 |
|------|------|------|
| `@McpTool` | 标记方法为 MCP 工具 | `name`: 工具名，`description`: 描述 |
| `@McpToolParam` | 标记工具参数 | `description`: 参数描述，`required`: 是否必需 |

### 4.4 服务端启动类

```java
package org.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("MCP 服务器已启动！");
        System.out.println("SSE 端点: http://localhost:8080/sse");
        System.out.println("========================================");
    }
}
```

---

## 五、MCP 客户端开发

### 5.1 客户端依赖

**mcp-client/pom.xml**

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

    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

### 5.2 客户端配置

**application.yml**

```yaml
spring:
  application:
    name: mcp-weather-client
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}      # 从环境变量读取
      base-url: https://ai.32zi.com   # 自定义 API 地址
      chat:
        options:
          model: claude-3-7-sonnet-20250219
    retry:
      max-attempts: 5
      backoff:
        initial-interval: 2000
        multiplier: 2
        max-interval: 60000
    mcp:
      client:
        enabled: true
        name: weather-client
        version: 1.0.0
        type: ASYNC
        request-timeout: 30s
        sse:
          connections:
            weather-server:
              url: http://localhost:8080
              sse-endpoint: /sse

server:
  port: 8081
```

### 5.3 客户端服务层

**WeatherMcpService.java**

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
public class WeatherMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public WeatherMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        // 从客户端列表中获取第一个
        this.mcpAsyncClient = mcpAsyncClients.isEmpty() 
            ? null 
            : mcpAsyncClients.get(0);
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        if (mcpAsyncClient == null) {
            System.err.println("MCP 客户端未初始化");
            return;
        }
        // 列出可用工具
        mcpAsyncClient.listTools()
            .doOnNext(tools -> {
                System.out.println("可用工具：" + tools.tools().stream()
                    .map(McpSchema.Tool::name)
                    .toList());
            })
            .subscribe();
    }

    public Mono<String> getCurrentWeather(String city) {
        return mcpAsyncClient.callTool(
            new McpSchema.CallToolRequest(
                "getCurrentWeather",
                Map.of("city", city)
            )
        ).map(this::extractResult);
    }

    public Mono<String> getWeatherForecast(String city, int days) {
        return mcpAsyncClient.callTool(
            new McpSchema.CallToolRequest(
                "getWeatherForecast",
                Map.of("city", city, "days", days)
            )
        ).map(this::extractResult);
    }

    public Mono<String> getAirQuality(String city) {
        return mcpAsyncClient.callTool(
            new McpSchema.CallToolRequest(
                "getAirQuality",
                Map.of("city", city)
            )
        ).map(this::extractResult);
    }

    public Mono<String> getLifeIndex(String city) {
        return mcpAsyncClient.callTool(
            new McpSchema.CallToolRequest(
                "getLifeIndex",
                Map.of("city", city)
            )
        ).map(this::extractResult);
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

**WeatherController.java**

```java
package org.example.client.controller;

import org.example.client.service.WeatherMcpService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherMcpService weatherMcpService;

    public WeatherController(WeatherMcpService weatherMcpService) {
        this.weatherMcpService = weatherMcpService;
    }

    @GetMapping("/current")
    public Mono<Map<String, String>> getCurrentWeather(@RequestParam String city) {
        return weatherMcpService.getCurrentWeather(city)
            .map(result -> Map.of("city", city, "data", result));
    }

    @GetMapping("/forecast")
    public Mono<Map<String, Object>> getWeatherForecast(
            @RequestParam String city,
            @RequestParam(defaultValue = "3") int days) {
        return weatherMcpService.getWeatherForecast(city, days)
            .map(result -> Map.of(
                "city", city, 
                "days", days, 
                "data", result
            ));
    }

    @GetMapping("/air-quality")
    public Mono<Map<String, String>> getAirQuality(@RequestParam String city) {
        return weatherMcpService.getAirQuality(city)
            .map(result -> Map.of("city", city, "data", result));
    }

    @GetMapping("/life-index")
    public Mono<Map<String, String>> getLifeIndex(@RequestParam String city) {
        return weatherMcpService.getLifeIndex(city)
            .map(result -> Map.of("city", city, "data", result));
    }
}
```

---

## 六、流式输出优化

### 6.1 为什么需要流式输出？

传统的 AI 调用需要等待完整响应才能返回，用户体验较差。流式输出（Streaming）可以让 AI 的回复像打字一样逐字显示，显著提升交互体验。

### 6.2 实现流式 AI 问答

在服务层添加流式方法：

```java
import reactor.core.publisher.Flux;

@Service
public class WeatherMcpService {
    
    // ... 其他代码

    /**
     * AI 智能天气问答（流式输出）
     */
    public Flux<String> askWeatherAIStream(String question) {
        String city = extractCityFromQuestion(question);

        // 并行获取天气数据
        return Mono.zip(
                getCurrentWeather(city),
                getAirQuality(city),
                getLifeIndex(city)
        ).flatMapMany(tuple -> {
            String currentWeather = tuple.getT1();
            String airQuality = tuple.getT2();
            String lifeIndex = tuple.getT3();

            String prompt = String.format(
                "基于以下%s的天气数据，回答用户问题。\n\n" +
                "天气数据：\n%s\n%s\n%s\n\n用户问题：%s",
                city, currentWeather, airQuality, lifeIndex, question
            );

            // 使用 stream() 开启流式输出
            return chatClientBuilder.build()
                    .prompt(prompt)
                    .stream()
                    .content();
        }).onErrorResume(e -> Flux.just("获取天气信息失败: " + e.getMessage()));
    }

    private String extractCityFromQuestion(String question) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州"};
        for (String city : cities) {
            if (question.contains(city)) return city;
        }
        return "北京";
    }
}
```

### 6.3 流式接口控制器

```java
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    
    // ... 其他代码

    /**
     * AI 智能天气问答（流式输出）
     */
    @PostMapping(
        value = "/ask/stream", 
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> askWeatherAIStream(
            @RequestBody Map<String, String> request) {
        String question = request.getOrDefault(
            "question", 
            "今天天气怎么样？"
        );
        return weatherMcpService.askWeatherAIStream(question);
    }
}
```

### 6.4 测试流式接口

```bash
curl -X POST http://localhost:8081/api/weather/ask/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "北京今天适合出门吗？"}'
```

响应将以 SSE 格式逐字返回，例如：

```
data: 根据

data: 北京

data: 今天的

data: 天气

data: ...
```

---

## 七、测试与验证

### 7.1 启动服务

**步骤 1：启动 MCP 服务端**

```bash
cd mcp-server
mvn spring-boot:run
```

看到以下输出表示启动成功：
```
MCP 服务器已启动！
SSE 端点: http://localhost:8080/sse
```

**步骤 2：启动 MCP 客户端**

```bash
cd mcp-client
mvn spring-boot:run
```

### 7.2 API 测试

| 功能 | 请求 | 示例 |
|------|------|------|
| 当前天气 | GET | `curl "http://localhost:8081/api/weather/current?city=北京"` |
| 天气预报 | GET | `curl "http://localhost:8081/api/weather/forecast?city=上海&days=3"` |
| 空气质量 | GET | `curl "http://localhost:8081/api/weather/air-quality?city=广州"` |
| 生活指数 | GET | `curl "http://localhost:8081/api/weather/life-index?city=深圳"` |
| AI 问答 | POST | `curl -X POST ... /ask/stream -d '{"question":"..."}'` |

### 7.3 预期响应示例

```json
{
  "city": "北京",
  "data": "【北京】当前天气：晴天，温度：25°C，更新时间：14:30:15"
}
```

---

## 八、常见问题与解决方案

### 8.1 参数名解析失败

**问题**：`java.lang.IllegalArgumentException: Name for argument of type [java.lang.String] not specified`

**原因**：编译器未保留参数名信息

**解决**：在 `pom.xml` 中添加编译器配置：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```


### 8.2 工具列表为空

**问题**：`tools=[]`

**原因**：服务器类型与方法返回类型不匹配

**解决**：
- `type: SYNC` → 同步方法（返回 `String`）
- `type: ASYNC` → 异步方法（返回 `Mono<String>`）

### 8.3 AI API 限流

**问题**：`429 Too Many Requests`

**解决**：增加重试间隔和最大重试次数：

```yaml
spring:
  ai:
    retry:
      max-attempts: 5
      backoff:
        initial-interval: 2000
        max-interval: 60000
```
**问题**：` 503 Service Unavailable from POST https://ai.32zi.com/v1/chat/completions`

**解决**：换模型，模型一定要支持工具调用：claude-3-7-sonnet-latest：

---

## 九、总结与扩展

### 9.1 核心知识点回顾

1. **MCP 协议**：AI 世界的"Type-C"，标准化工具集成
2. **Spring AI MCP**：注解驱动，自动配置，响应式支持
3. **异步编程**：`Mono<T>` 和 `Flux<T>` 处理非阻塞操作
4. **流式输出**：提升用户体验的关键技术

### 9.2 项目扩展建议

1. **增加缓存**：使用 Redis 缓存天气数据，减少 API 调用
2. **添加认证**：为 MCP 服务端添加 OAuth2 认证
3. **多服务器支持**：配置多个 MCP 服务器，实现负载均衡
4. **WebSocket 支持**：将流式输出改为 WebSocket 实时推送
5. **前端界面**：开发 React/Vue 前端，实现可视化交互

### 9.3 参考资源

- [Spring AI MCP 官方文档](https://docs.springframework.org.cn/spring-ai/reference/api/mcp/mcp-overview.html)
- [MCP 协议规范](https://modelcontextprotocol.io/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)

---

*本文完。如有问题，欢迎在评论区留言交流！*

**原创声明**：本文为原创教程，转载请注明出处。

---