# Spring AI 结构化输出转换器实战：告别字符串解析，拥抱类型安全

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-4](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-4)

## 引言

在使用大语言模型（LLM）开发应用时，我们经常会遇到这样的痛点：AI 返回的响应是**纯文本字符串**，需要手动解析才能提取有用的信息。这不仅繁琐，还容易出错，特别是当需要处理复杂的数据结构时。

**Spring AI 结构化输出转换器（Structured Output Converter）** 正是为了解决这一问题而生。它允许我们将 AI 的响应**自动转换为 Java 对象**（Bean、Map、List），实现类型安全的 AI 响应处理。

**本文你将学到**：
- 结构化输出转换器的核心概念与工作原理
- `BeanOutputConverter`、`MapOutputConverter`、`ListOutputConverter` 三种转换器的实战应用
- 如何在响应式编程（Reactor）中优雅地使用转换器
- 完整的项目实战与 API 测试示例

---

## 目录

- [一、项目概述与环境准备](#一项目概述与环境准备)
- [二、核心概念：什么是结构化输出转换器](#二核心概念什么是结构化输出转换器)
- [三、三种转换器详解与实战](#三种转换器详解与实战)
  - [3.1 BeanOutputConverter - Java Bean 自动映射](#31-beanoutputconverter---java-bean-自动映射)
  - [3.2 MapOutputConverter - 灵活的键值对结构](#32-mapoutputconverter---灵活的键值对结构)
  - [3.3 ListOutputConverter - 列表数据处理](#33-listoutputconverter---列表数据处理)
- [四、项目结构详解](#四项目结构详解)
- [五、API 测试与效果展示](#五api-测试与效果展示)
- [六、避坑指南与最佳实践](#六避坑指南与最佳实践)
- [七、总结与扩展思考](#七总结与扩展思考)

---

## 一、项目概述与环境准备

### 1.1 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.1.0-SNAPSHOT | AI 开发框架 |
| Java | 25 | 编程语言 |
| Maven | - | 构建工具 |
| WebFlux | - | 响应式 Web 框架 |

### 1.2 项目结构

```
phase-4/
├── src/main/java/org/example/
│   ├── SpringAiJcStart.java          # 启动类
│   ├── controller/
│   │   └── StructuredOutputController.java  # REST API 控制器
│   ├── service/
│   │   └── StructuredOutputService.java     # 业务逻辑服务
│   ├── entity/
│   │   ├── MovieActor.java           # 演员实体类
│   │   └── ProductInfo.java          # 产品信息实体类
│   └── exception/
│       ├── ChatException.java
│       ├── ErrorResponse.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml               # 配置文件
└── pom.xml                           # Maven 配置
```

[建议：此处插入项目结构截图，展示 IDE 中的目录层级]

### 1.3 核心依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux - 响应式编程支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Spring Boot Web - 嵌入式 Netty -->
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
    
    <!-- Spring AI OpenAI Starter - AI 模型支持 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

### 1.4 配置文件

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://ai.32zi.com  # 使用 32ai 代理服务
      chat:
        options:
          model: claude-3-7-sonnet-latest
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000
        multiplier: 2
        max-interval: 10000
  server:
    port: 8080
    netty:
      connection-timeout: 2s
```

---

## 二、核心概念：什么是结构化输出转换器

### 2.1 传统方式的痛点

在没有结构化输出转换器之前，处理 AI 响应通常是这样的：

```java
// 传统方式：手动解析字符串
String response = chatClient.prompt("列出周杰伦的5部电影")
    .call()
    .content();

// 需要手动解析 JSON 字符串，容易出错
// 响应格式不稳定，可能需要复杂的正则表达式
```

### 2.2 结构化输出转换器的优势

**Spring AI 结构化输出转换器** 通过以下机制解决上述问题：

1. **JSON Schema 生成**：自动根据目标类型生成 JSON Schema，指导 AI 模型输出符合预期的格式
2. **自动类型转换**：将 AI 的 JSON 响应自动映射到 Java 对象
3. **类型安全**：编译期类型检查，避免运行时类型错误
4. **简化代码**：无需手动解析，代码更简洁易维护

### 2.3 三种转换器对比

| 转换器 | 适用场景 | 输出类型 | 特点 |
|--------|----------|----------|------|
| `BeanOutputConverter` | 固定结构的数据 | Java Bean | 自动生成 JSON Schema，强类型 |
| `MapOutputConverter` | 灵活/动态结构 | `Map<String, Object>` | 无需预定义类，灵活度高 |
| `ListOutputConverter` | 列表数据 | `List<String>` | 处理逗号分隔的列表 |

### 2.4 支持的 AI 模型

以下 AI 模型已测试支持列表、映射和 Bean 结构化输出：

| 模型厂商 | 具体模型/系列 | 说明 |
|----------|---------------|------|
| **OpenAI** | GPT-4、GPT-4o、GPT-3.5-Turbo | 原生支持结构化输出，JSON Schema 遵循度高 |
| **Anthropic** | Claude 3 系列（Opus、Sonnet、Haiku） | 优秀的指令遵循能力，支持复杂嵌套结构 |
| **Azure OpenAI** | GPT-4、GPT-3.5-Turbo | 企业级部署，与 OpenAI 接口兼容 |
| **Mistral AI** | Mistral Large、Medium、Small | 欧洲领先模型，性价比优秀 |
| **Ollama** | Llama 3、Mistral、Gemma 等本地模型 | 本地部署，保护数据隐私 |
| **Vertex AI** | Gemini Pro、Gemini Ultra | Google 云端模型，多模态能力强 |

**提示**：不同模型对 JSON Schema 的遵循程度有所差异。对于复杂结构，推荐使用 **Claude 3** 或 **GPT-4** 系列模型以获得最佳效果。

### 2.5 内置 JSON 模式配置

一些 AI 模型提供专门的配置选项来生成结构化（通常是 JSON）输出，这比单纯依赖提示词更可靠：

| 模型厂商 | 配置选项 | 说明 | 配置示例 |
|----------|----------|------|----------|
| **OpenAI** | `spring.ai.openai.chat.options.responseFormat` | 支持 `JSON_OBJECT`（保证有效 JSON）和 `JSON_SCHEMA`（保证符合提供的模式） | `{"type": "json_schema", "schema": {...}}` |
| **Azure OpenAI** | `spring.ai.azure.openai.chat.options.responseFormat` | 设置为 `{"type": "json_object"}` 启用 JSON 模式 | `{"type": "json_object"}` |
| **Ollama** | `spring.ai.ollama.chat.options.format` | 目前唯一接受的值是 `json` | `json` |
| **Mistral AI** | `spring.ai.mistralai.chat.options.responseFormat` | 设置为 `{"type": "json_object"}` 启用 JSON 模式 | `{"type": "json_object"}` |

#### OpenAI 结构化输出配置示例

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      chat:
        options:
          model: gpt-4o
          # 启用 JSON 模式，确保输出有效 JSON
          response-format:
            type: json_object
```

#### Ollama JSON 模式配置示例

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3
          # 强制输出 JSON 格式
          format: json
```

**重要提示**：
- 使用内置 JSON 模式时，**必须在提示词中明确说明要返回 JSON 格式**
- `JSON_SCHEMA` 模式比 `JSON_OBJECT` 更严格，能确保输出符合指定结构
- 结合 `BeanOutputConverter` 使用时，内置 JSON 模式可以显著提高输出稳定性

---

## 三、三种转换器详解与实战

### 3.1 BeanOutputConverter - Java Bean 自动映射

**`BeanOutputConverter`** 是最常用的转换器，它可以将 AI 的 JSON 响应自动映射到预定义的 Java 类。

#### 3.1.1 定义实体类

首先，我们需要定义一个 Java Bean 来接收转换后的数据：

```java
package org.example.entity;

import java.util.List;

/**
 * 电影演员实体类 - 用于演示 BeanOutputConverter
 */
public class MovieActor {

    private String actor;           // 演员姓名
    private List<String> movies;    // 电影列表
    private List<String> awards;    // 奖项列表

    // 必须提供无参构造器
    public MovieActor() {
    }

    public MovieActor(String actor, List<String> movies, List<String> awards) {
        this.actor = actor;
        this.movies = movies;
        this.awards = awards;
    }

    // Getters 和 Setters
    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public List<String> getMovies() {
        return movies;
    }

    public void setMovies(List<String> movies) {
        this.movies = movies;
    }

    public List<String> getAwards() {
        return awards;
    }

    public void setAwards(List<String> awards) {
        this.awards = awards;
    }
}
```

**关键点**：
- 必须提供**无参构造器**，否则转换器无法实例化对象
- 属性名应与 AI 返回的 JSON 字段名对应
- 支持嵌套对象和集合类型

#### 3.1.2 服务层实现

```java
@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    public StructuredOutputService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 使用 BeanOutputConverter 获取演员电影信息
     */
    public Mono<MovieActor> getActorMovies(String actorName) {
        return Mono.fromCallable(() -> {
            // 1. 创建 BeanOutputConverter，指定目标类型
            BeanOutputConverter<MovieActor> converter = 
                new BeanOutputConverter<>(MovieActor.class);

            // 2. 获取格式化指令（JSON Schema）
            String format = converter.getFormat();
            logger.debug("生成的格式指令: {}", format);

            // 3. 构建提示词模板，包含格式占位符
            String userPrompt = """
                    为演员 {actor} 生成电影作品信息。
                    包含该演员最著名的5部电影和获得的3个重要奖项。
                    {format}
                    """;

            // 4. 使用 PromptTemplate 构建提示词
            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("actor", actorName, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            // 5. 调用 AI 模型
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            logger.debug("AI 原始响应: {}", response);

            // 6. 使用转换器将 JSON 响应转换为 MovieActor 对象
            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

**核心流程解析**：

1. **创建转换器**：`new BeanOutputConverter<>(MovieActor.class)` 会根据 `MovieActor` 类生成对应的 JSON Schema
2. **获取格式指令**：`converter.getFormat()` 返回 JSON Schema，用于指导 AI 输出格式
3. **构建提示词**：将 `{format}` 占位符替换为实际的 Schema 指令
4. **调用 AI**：发送提示词并获取响应
5. **类型转换**：`converter.convert(response)` 自动将 JSON 字符串转为 Java 对象

#### 3.1.3 控制器 API

```java
@RestController
@RequestMapping("/api/structured")
public class StructuredOutputController {

    private final StructuredOutputService structuredOutputService;

    public StructuredOutputController(StructuredOutputService structuredOutputService) {
        this.structuredOutputService = structuredOutputService;
    }

    /**
     * 获取演员电影信息 - BeanOutputConverter 示例
     */
    @GetMapping("/actor")
    public Mono<ResponseEntity<MovieActor>> getActorMovies(
            @RequestParam String actorName) {
        
        return structuredOutputService.getActorMovies(actorName)
                .map(ResponseEntity::ok);
    }
}
```

#### 3.1.4 测试示例

```bash
# 请求
curl "http://localhost:8080/api/structured/actor?actorName=成龙"

# 响应
{
    "actor": "成龙",
    "movies": [
        "警察故事",
        "醉拳",
        "尖峰时刻",
        "红番区",
        "A计划"
    ],
    "awards": [
        "奥斯卡终身成就奖",
        "金马奖最佳男主角",
        "香港电影金像奖"
    ]
}
```

---

### 3.2 MapOutputConverter - 灵活的键值对结构

**`MapOutputConverter`** 适用于数据结构不固定或需要动态解析的场景，它将 AI 响应转换为 `Map<String, Object>`。

#### 3.2.1 服务层实现

```java
/**
 * 使用 MapOutputConverter 获取灵活的键值对数据
 */
public Mono<Map<String, Object>> analyzeTopic(String topic) {
    return Mono.fromCallable(() -> {
        // 创建 MapOutputConverter
        MapOutputConverter converter = new MapOutputConverter();

        String format = converter.getFormat();

        String userPrompt = """
                分析以下主题，并以键值对形式返回相关信息：
                主题：{topic}
                
                请返回以下信息（JSON格式）：
                - 定义（definition）
                - 重要性（importance）
                - 相关概念（relatedConcepts，数组形式）
                - 应用场景（applications，数组形式）
                
                {format}
                """;

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(userPrompt)
                .variables(Map.of("topic", topic, "format", format))
                .build();

        Prompt prompt = new Prompt(promptTemplate.createMessage());

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        // 转换为 Map
        return converter.convert(response);
    }).subscribeOn(Schedulers.boundedElastic());
}
```

#### 3.2.2 控制器 API

```java
/**
 * 分析主题 - MapOutputConverter 示例
 */
@GetMapping("/topic")
public Mono<ResponseEntity<Map<String, Object>>> analyzeTopic(
        @RequestParam String topic) {
    
    return structuredOutputService.analyzeTopic(topic)
            .map(ResponseEntity::ok);
}
```

#### 3.2.3 测试示例

```bash
# 请求
curl "http://localhost:8080/api/structured/topic?topic=人工智能"

# 响应
{
    "definition": "人工智能是计算机科学的一个分支，致力于创造能够模拟人类智能的系统",
    "importance": "AI 正在改变各行各业，从医疗诊断到自动驾驶，具有巨大的经济和社会影响",
    "relatedConcepts": ["机器学习", "深度学习", "神经网络", "自然语言处理"],
    "applications": ["智能助手", "图像识别", "推荐系统", "自动驾驶"]
}
```

**适用场景**：
- 数据结构动态变化
- 快速原型开发，不想定义实体类
- 需要灵活处理不同格式的响应

---

### 3.3 ListOutputConverter - 列表数据处理

**`ListOutputConverter`** 专门用于处理逗号分隔的列表数据，将 AI 响应转换为 `List<String>`。

#### 3.3.1 服务层实现

```java
/**
 * 使用 ListOutputConverter 获取列表数据
 */
public Mono<List<String>> getSuggestions(String category, int count) {
    return Mono.fromCallable(() -> {
        // 创建 ListOutputConverter，需要传入 ConversionService
        ListOutputConverter converter = 
            new ListOutputConverter(new DefaultConversionService());

        String format = converter.getFormat();

        String userPrompt = """
                列出 {count} 个关于 {category} 的建议。
                请以逗号分隔的列表形式返回。
                {format}
                """;

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(userPrompt)
                .variables(Map.of(
                    "count", String.valueOf(count), 
                    "category", category, 
                    "format", format))
                .build();

        Prompt prompt = new Prompt(promptTemplate.createMessage());

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        // 转换为 List
        return converter.convert(response);
    }).subscribeOn(Schedulers.boundedElastic());
}
```

#### 3.3.2 控制器 API

```java
/**
 * 获取建议列表 - ListOutputConverter 示例
 */
@GetMapping("/suggestions")
public Mono<ResponseEntity<List<String>>> getSuggestions(
        @RequestParam String category,
        @RequestParam(defaultValue = "5") int count) {
    
    return structuredOutputService.getSuggestions(category, count)
            .map(ResponseEntity::ok);
}
```

#### 3.3.3 测试示例

```bash
# 请求
curl "http://localhost:8080/api/structured/suggestions?category=Java学习资源&count=5"

# 响应
[
    "《Effective Java》书籍",
    "Spring 官方文档",
    "Baeldung 教程网站",
    "LeetCode 算法练习",
    "GitHub 开源项目实践"
]
```

---

## 四、项目结构详解

### 4.1 实体类设计

```java
// ProductInfo.java - 产品信息实体
public class ProductInfo {
    private String name;        // 产品名称
    private String description; // 产品描述
    private double price;       // 价格
    private String category;    // 类别
    private int stock;          // 库存
    
    // 必须有无参构造器
    public ProductInfo() {}
    
    // Getters and Setters...
}
```

### 4.2 异常处理

```java
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
}
```

### 4.3 响应式编程处理

本项目使用 **Project Reactor** 进行响应式编程：

```java
public Mono<MovieActor> getActorMovies(String actorName) {
    return Mono.fromCallable(() -> {
        // 同步代码块
        BeanOutputConverter<MovieActor> converter = 
            new BeanOutputConverter<>(MovieActor.class);
        // ... 业务逻辑
        return converter.convert(response);
    })
    .subscribeOn(Schedulers.boundedElastic()); // 在弹性线程池执行
}
```

**为什么使用 `Schedulers.boundedElastic()`？**

- AI 调用是阻塞 I/O 操作
- `boundedElastic()` 提供了有界的弹性线程池
- 防止阻塞事件循环线程，保持应用响应性

---

## 五、API 测试与效果展示

### 5.1 完整 API 列表

| 端点 | 方法 | 参数 | 说明 |
|------|------|------|------|
| `/api/structured/actor` | GET | `actorName` | 获取演员电影信息（Bean） |
| `/api/structured/product` | POST | `description` | 生成产品信息（Bean） |
| `/api/structured/topic` | GET | `topic` | 分析主题（Map） |
| `/api/structured/compare` | GET | `product1`, `product2` | 产品对比（Map） |
| `/api/structured/suggestions` | GET | `category`, `count` | 获取建议列表（List） |
| `/api/structured/keywords` | POST | `text`, `count` | 提取关键词（List） |
| `/api/structured/steps` | GET | `task` | 获取任务步骤（List） |

### 5.2 测试示例

[建议：此处插入 Postman 或 curl 测试截图，展示 API 调用过程和响应结果]

```bash
# 1. 测试 BeanOutputConverter
curl "http://localhost:8080/api/structured/actor?actorName=周星驰"

# 2. 测试 MapOutputConverter
curl "http://localhost:8080/api/structured/topic?topic=微服务架构"

# 3. 测试 ListOutputConverter
curl "http://localhost:8080/api/structured/suggestions?category=编程语言&count=3"

# 4. POST 请求测试
curl -X POST "http://localhost:8080/api/structured/product" \
  -H "Content-Type: application/json" \
  -d '{"description": "一款适合程序员的机械键盘"}'
```

---

## 六、避坑指南与最佳实践

### 6.1 常见问题与解决方案

#### 问题 1：实体类缺少无参构造器

```
错误：Cannot construct instance of `org.example.entity.MovieActor`
```

**解决方案**：确保所有用于 `BeanOutputConverter` 的类都有无参构造器：

```java
public class MovieActor {
    // 必须提供！
    public MovieActor() {
    }
}
```

#### 问题 2：AI 响应格式不符合预期

**解决方案**：
1. 检查 `converter.getFormat()` 是否正确插入到提示词中
2. 在提示词中明确指定输出格式要求
3. 使用更强大的模型（如 GPT-4、Claude 3.5+）

#### 问题 3：响应式编程中的线程阻塞

**解决方案**：始终使用 `subscribeOn(Schedulers.boundedElastic())` 包装阻塞操作：

```java
return Mono.fromCallable(() -> {
    // 阻塞操作
    return result;
}).subscribeOn(Schedulers.boundedElastic());
```

### 6.2 最佳实践

1. **实体类设计**
   - 始终提供无参构造器
   - 使用包装类型（`Integer` 而非 `int`）避免默认值问题
   - 添加字段验证注解（如 `@NotNull`）

2. **提示词工程**
   - 在提示词中明确说明期望的输出格式
   - 提供示例（Few-shot）帮助 AI 理解要求
   - 使用 `{format}` 占位符插入 JSON Schema

3. **错误处理**
   - 添加全局异常处理器
   - 记录 AI 原始响应便于调试
   - 实现重试机制（Spring AI 已内置）

4. **性能优化**
   - 使用响应式编程处理并发请求
   - 考虑缓存频繁查询的结果
   - 合理设置超时时间

---

## 七、总结与扩展思考

### 7.1 核心要点回顾

本文详细介绍了 Spring AI 结构化输出转换器的三种实现：

- **BeanOutputConverter**：将 AI 响应映射到 Java Bean，适合固定结构的数据
- **MapOutputConverter**：转换为灵活的 Map 结构，适合动态数据
- **ListOutputConverter**：处理逗号分隔的列表数据

通过使用这些转换器，我们可以：
- 告别繁琐的字符串解析
- 实现类型安全的 AI 响应处理
- 大幅提升开发效率和代码可维护性

### 7.2 扩展思考

基于本项目，你可以进一步探索：

1. **复杂嵌套对象**：尝试定义包含嵌套对象的实体类，如 `Order` 包含 `List<OrderItem>`
2. **自定义转换器**：实现 `StructuredOutputConverter` 接口，创建特定领域的转换器
3. **流式响应**：结合 `ChatClient` 的流式 API，实现实时结构化输出
4. **多模型对比**：测试不同 AI 模型对结构化输出的支持程度
5. **前端集成**：开发一个可视化界面，动态展示不同类型的结构化输出

### 7.3 参考资料

- [Spring AI 官方文档 - 结构化输出转换器](https://docs.springframework.org.cn/spring-ai/reference/api/structured-output-converter.html)
- [Spring AI GitHub 仓库](https://github.com/spring-projects/spring-ai)
- [Project Reactor 文档](https://projectreactor.io/docs)

---

**标签建议**：
- CSDN：`Java`、`Spring Boot`、`Spring AI`、`人工智能`、`实战`
- 稀土掘金：`后端`、`Spring AI`、`AI 应用开发`、`Java`

---

💰 **为什么选择 32ai？**

**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

<!-- 互动提示 -->
欢迎在评论区交流讨论！

**原创声明**：本文为原创教程，转载请注明出处
