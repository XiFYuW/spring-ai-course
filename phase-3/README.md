# Spring AI 实战：构建支持多会话管理与动态角色的智能聊天服务

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-3](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-3)

## 引言

在AI应用爆发的今天，**多会话管理**、**上下文感知**和**动态角色配置**是构建生产级AI聊天系统的三大核心能力。本文将带你深入实战，基于 **Spring Boot 3.x + Spring AI + R2DBC** 技术栈，从零构建一个功能完善的智能聊天服务。

### 🎯 项目核心亮点

| 特性 | 说明 |
|------|------|
| **🔥 动态角色系统** | 数据库驱动的角色配置，支持热更新、模板变量、多角色切换 |
| **💬 多会话管理** | 独立会话隔离，支持创建、查询、删除会话 |
| **🧠 上下文感知** | 自动维护对话历史，智能上下文长度控制 |
| **⚡ 响应式架构** | WebFlux + R2DBC 全异步非阻塞设计 |

**你将收获：**
- 掌握 **Spring AI** 的核心API和最佳实践
- 理解 **响应式编程** 在AI服务中的应用
- 学会设计 **可扩展的多会话架构**
- 实现 **企业级动态角色配置系统**（支持热更新、模板渲染、优先级管理）

---

## 目录

- [一、项目概述与技术栈](#一项目概述与技术栈)
- [二、环境准备](#二环境准备)
- [三、核心概念解析](#三核心概念解析)
- [四、动态角色系统详解](#四动态角色系统详解)
- [五、项目架构设计](#五项目架构设计)
- [六、代码实现详解](#六代码实现详解)
- [七、API使用指南](#七api使用指南)
- [八、避坑指南与优化建议](#八避坑指南与优化建议)
- [九、总结与扩展](#九总结与扩展)

---

## 一、项目概述与技术栈

### 1.1 项目功能特性

#### 🔥 核心特性：动态角色系统

本项目最大的亮点是**数据库驱动的动态角色配置系统**，彻底解决了传统AI应用硬编码提示词的痛点：

| 特性 | 传统方式 | 本项目方案 |
|------|----------|------------|
| **配置方式** | 硬编码在Java文件中 | 数据库存储，支持热更新 |
| **角色切换** | 需要重启服务 | 实时切换，即时生效 |
| **模板支持** | 固定文本 | 支持变量替换 `{roleName}` |
| **多租户** | 难以支持 | 天然支持，可按租户隔离 |
| **管理界面** | 无 | 提供完整CRUD API |

**内置角色示例：**
- 👨‍💻 **技术专家** - 专注于技术问题，提供代码示例和最佳实践
- ✍️ **创意写作** - 富有想象力，擅长故事创作和文案撰写
- 🌐 **语言翻译** - 专业翻译，考虑文化差异和语境
- 🤖 **默认助手** - 通用型AI助手，简洁专业的回答风格

#### 📋 其他功能特性

| 特性 | 说明 |
|------|------|
| **多会话管理** | 支持创建、查询、删除多个独立聊天会话 |
| **上下文感知** | 自动维护对话历史，智能上下文长度控制（滑动窗口策略） |
| **流式响应** | 支持SSE流式输出，打字机效果提升用户体验 |
| **响应式架构** | 基于WebFlux + R2DBC的全异步非阻塞设计，高并发友好 |

### 1.2 技术栈选型

```
┌─────────────────────────────────────────────────────────┐
│  表现层: Spring Boot 3.5.10 + WebFlux (响应式Web)       │
├─────────────────────────────────────────────────────────┤
│  AI层: Spring AI 1.1.0-SNAPSHOT (OpenAI适配器)          │
├─────────────────────────────────────────────────────────┤
│  数据层: Spring Data R2DBC + PostgreSQL (响应式数据库)  │
├─────────────────────────────────────────────────────────┤
│  配置层: 外部化配置 + 动态角色模板系统                    │
└─────────────────────────────────────────────────────────┘
```

**为什么选择这些技术？**
- **WebFlux**: 相比传统MVC，响应式编程更适合AI这种IO密集型场景
- **R2DBC**: 与WebFlux完美配合，实现全链路异步
- **Spring AI**: 官方支持的AI抽象层，屏蔽底层模型差异
- **为什么选择 32ai[点击访问](https://ai.32zi.com)？**: 低至 0.56 : 1 比率，直连、无需魔法。

---

## 二、环境准备

### 2.1 前置条件

- JDK 25+
- Maven 3.8+
- PostgreSQL 14+
- 可用的OpenAI API密钥（或兼容的API服务）

### 2.2 数据库初始化

执行 `src/main/resources/schema.sql` 中的SQL脚本，创建所需表结构：

```sql
-- 会话表
CREATE TABLE IF NOT EXISTS conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色配置表
CREATE TABLE IF NOT EXISTS role_config (
    id BIGSERIAL PRIMARY KEY,
    role_type VARCHAR(20) NOT NULL,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    template_content TEXT,
    enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 10,
    tags VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2.3 配置文件

编辑 `application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://ai.32zi.com  # 或使用兼容服务
      chat:
        options:
          model: claude-haiku-4-5-20251001
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000
        multiplier: 2
        max-interval: 10000
  
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/chatdb
    username: postgres
    password: your-password
    pool:
      enabled: true
      initial-size: 5
      max-size: 20

# 应用自定义配置
app:
  chat:
    system-prompt: "你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"
    max-context-pairs: 20  # 最大保留20轮对话
```

---

## 三、核心概念解析

### 3.1 Spring AI 消息模型

Spring AI 定义了四种消息类型，对应OpenAI的Message角色：

```java
// Spring AI 消息类型枚举
public enum MessageType {
    USER("user"),           // 用户消息
    ASSISTANT("assistant"), // AI助手回复
    SYSTEM("system"),       // 系统提示词
    TOOL("tool");           // 工具调用结果
}
```

**对话流程示意：**
```
[系统消息] → [用户消息1] → [助手回复1] → [用户消息2] → [助手回复2] → ...
```

### 3.2 响应式编程核心

本项目全面使用 **Project Reactor** 的 Mono 和 Flux：

| 类型 | 说明 | 使用场景 |
|------|------|----------|
| `Mono<T>` | 0或1个元素的异步序列 | 单条数据查询、保存操作 |
| `Flux<T>` | 0到N个元素的异步序列 | 列表查询、流式响应 |

**关键操作符：**
- `flatMap`: 异步转换，处理嵌套Mono
- `map`: 同步转换
- `then`: 完成前一个操作后执行下一个
- `onErrorResume`: 错误处理

### 3.3 上下文长度控制策略

为避免Token超限，我们采用**滑动窗口**策略：

```java
// 只保留最近 maxContextMessages 条消息
if (messages.size() > maxContextMessages) {
    contextMessages = messages.subList(
        messages.size() - maxContextMessages, 
        messages.size()
    );
}
```

---

## 四、动态角色系统详解

> **本章是项目的核心亮点**，我们将深入讲解如何设计一个灵活、可扩展的动态角色配置系统，让AI助手能够根据不同的场景和用户需求，动态切换角色和行为模式。

### 4.1 为什么需要动态角色系统？

在传统的AI聊天应用中，系统提示词（System Prompt）通常是硬编码的：

```java
// 传统方式：硬编码系统提示词
String systemPrompt = "你是一个友好、专业的AI助手...";
```

**这种设计的痛点：**
- ❌ 无法根据场景切换AI角色（技术专家 vs 创意写手）
- ❌ 修改提示词需要重新部署
- ❌ 无法支持多租户场景（不同用户使用不同角色）
- ❌ 提示词管理混乱，无法复用

**动态角色系统的优势：**
- ✅ 数据库管理角色配置，热更新无需重启
- ✅ 支持多角色并存，按需切换
- ✅ 模板变量支持，灵活渲染内容
- ✅ 标签分类管理，便于检索

### 4.2 Spring AI 角色模型

Spring AI 定义了四种标准角色类型，对应 OpenAI 的 Message 角色：

| 角色类型 | 说明 | 使用场景 |
|----------|------|----------|
| `SYSTEM` | 系统角色 | 定义AI的行为准则、回答风格 |
| `USER` | 用户角色 | 用户的提问内容 |
| `ASSISTANT` | 助手角色 | AI的回复内容 |
| `TOOL` | 工具角色 | 函数调用的结果返回 |

**对话消息流：**
```
┌─────────────────────────────────────────────────────────────┐
│  [SYSTEM] 定义AI身份和行为准则                                │
│      ↓                                                       │
│  [USER] 用户提问："解释Java中的Mono"                         │
│      ↓                                                       │
│  [ASSISTANT] AI回复：详细解释Mono的概念...                    │
│      ↓                                                       │
│  [USER] 用户追问："和Flux有什么区别？"                       │
│      ↓                                                       │
│  [ASSISTANT] AI回复：对比说明两者的区别...                    │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 角色配置数据模型

#### 4.3.1 数据库表结构

```sql
CREATE TABLE IF NOT EXISTS role_config (
    id BIGSERIAL PRIMARY KEY,
    role_type VARCHAR(20) NOT NULL,      -- 角色类型：system/user/assistant/tool
    role_name VARCHAR(100) NOT NULL UNIQUE, -- 角色名称：如"技术专家"
    description TEXT,                     -- 角色描述
    template_content TEXT,                -- 模板内容（支持变量替换）
    enabled BOOLEAN DEFAULT true,         -- 是否启用
    priority INTEGER DEFAULT 10,          -- 优先级（数值越小优先级越高）
    tags VARCHAR(200),                    -- 标签，如"technical,programming"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.3.2 字段详解

| 字段 | 说明 | 示例 |
|------|------|------|
| `role_type` | 对应Spring AI的MessageType | `system`, `user`, `assistant` |
| `role_name` | 人类可读的角色名称 | `技术专家`, `创意写手` |
| `template_content` | 支持变量替换的模板 | `你是{roleName}，{description}` |
| `priority` | 同类型角色的加载顺序 | 1表示最高优先级 |
| `tags` | 用于分类和筛选 | `technical,java,spring` |

### 4.4 内置角色配置示例

项目初始化时会自动插入以下默认角色：

#### 4.4.1 系统角色（SYSTEM）

```sql
-- 1. 默认助手
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('system', '默认助手', '友好的AI助手，提供专业、简洁的回答', 
        '你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。', 
        true, 1, 'general,default');

-- 2. 技术专家
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('system', '技术专家', '专注于技术问题的AI助手', 
        '# {roleName}
{roleDescription}

## 核心能力
- 深入分析技术问题
- 提供详细的代码示例
- 解释技术原理和最佳实践

## 回答风格
请以专业、严谨的态度回答技术问题，确保信息的准确性和实用性。

系统提示：{systemPrompt}', 
        true, 2, 'technical,programming');

-- 3. 创意写作
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('system', '创意写作', '专注于创意写作的AI助手', 
        '# {roleName}
{roleDescription}

## 创作风格
- 富有想象力和创造力
- 语言生动、形象
- 注重情感表达和故事性

## 适用场景
- 故事创作
- 诗歌写作
- 文案创作', 
        true, 3, 'creative,writing');

-- 4. 语言翻译
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('system', '语言翻译', '多语言翻译助手', 
        '# {roleName}
{roleDescription}

## 翻译原则
- 准确传达原文意思
- 保持语言流畅自然
- 考虑文化差异和语境

## 支持语言
- 中文 ↔ 英文
- 其他语言翻译', 
        true, 4, 'translation,language');
```

#### 4.4.2 用户角色（USER）

```sql
-- 普通用户提问模板
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('user', '普通用户', '普通用户的提问模板', '{userQuestion}', true, 1, 'general,default');
```

#### 4.4.3 助手角色（ASSISTANT）

```sql
-- 标准回答模板
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) 
VALUES ('assistant', '标准回答', 'AI助手的标准回答模板', '{assistantResponse}', true, 1, 'general,default');
```

### 4.5 模板变量系统

#### 4.5.1 内置变量

系统会自动注入以下变量到模板中：

| 变量名 | 说明 | 来源 |
|--------|------|------|
| `roleName` | 角色名称 | role_config.role_name |
| `roleDescription` | 角色描述 | role_config.description |
| `roleType` | 角色类型 | role_config.role_type |
| `systemPrompt` | 系统提示词 | app.chat.system-prompt |

#### 4.5.2 模板渲染示例

**模板内容：**
```markdown
# {roleName}
{roleDescription}

## 核心能力
- 深入分析技术问题
- 提供详细的代码示例

系统提示：{systemPrompt}
```

**渲染结果：**
```markdown
# 技术专家
专注于技术问题的AI助手

## 核心能力
- 深入分析技术问题
- 提供详细的代码示例

系统提示：你是一个友好、专业的AI助手...
```

### 4.6 角色选择策略

#### 4.6.1 默认角色选择

当用户未指定角色时，系统按以下策略选择：

```java
private Mono<List<Message>> buildDynamicRoleMessages(Long roleId) {
    Flux<RoleConfig> roleConfigsFlux;
    
    if (roleId != null) {
        // 策略1：用户指定了角色ID，使用该角色
        roleConfigsFlux = roleConfigService.getRoleConfigById(roleId)
                .filter(RoleConfig::enabled)
                .flux();
    } else {
        // 策略2：使用默认助手角色
        roleConfigsFlux = roleConfigService.getRoleConfigByRoleName("默认助手")
                .filter(RoleConfig::enabled)
                .flux()
                .switchIfEmpty(roleConfigService.getAllEnabledRoleConfigs());
    }
    // ...
}
```

#### 4.6.2 多角色组合

支持同时启用多个角色配置，实现复杂的AI行为：

```java
// 获取所有启用的SYSTEM类型角色
Flux<RoleConfig> systemRoles = roleConfigService
        .getEnabledRoleConfigsByType("system");

// 按优先级排序后组合
return systemRoles
        .sort(Comparator.comparingInt(RoleConfig::priority))
        .map(role -> createMessageByRoleType(role.roleType(), role.templateContent()));
```

### 4.7 角色管理API

#### 4.7.1 创建角色

```bash
curl -X POST http://localhost:8080/api/role-configs \
  -H "Content-Type: application/json" \
  -d '{
    "roleType": "system",
    "roleName": "产品经理",
    "description": "专注于产品规划和需求分析的AI助手",
    "templateContent": "你是{roleName}，{roleDescription}。请从产品经理的角度分析问题。",
    "enabled": true,
    "priority": 5,
    "tags": "product,pm,requirement"
  }'
```

#### 4.7.2 查询角色列表

```bash
# 获取所有角色
curl http://localhost:8080/api/role-configs

# 按类型筛选
curl http://localhost:8080/api/role-configs?roleType=system

# 按标签筛选
curl http://localhost:8080/api/role-configs?tag=technical
```

#### 4.7.3 更新角色

```bash
curl -X PUT http://localhost:8080/api/role-configs/1 \
  -H "Content-Type: application/json" \
  -d '{
    "templateContent": "更新后的模板内容...",
    "enabled": true
  }'
```

#### 4.7.4 删除角色

```bash
curl -X DELETE http://localhost:8080/api/role-configs/1
```

### 4.8 使用指定角色聊天

#### 4.8.1 普通聊天使用角色

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "帮我设计一个电商系统的订单模块",
    "roleId": 2  // 使用"技术专家"角色
  }'
```

#### 4.8.2 流式聊天使用角色

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "写一个关于未来世界的故事",
    "roleId": 3  // 使用"创意写作"角色
  }'
```

### 4.9 角色系统设计亮点

| 特性 | 实现方式 | 价值 |
|------|----------|------|
| **热更新** | 数据库管理 + 实时查询 | 无需重启服务即可更新角色 |
| **模板引擎** | Spring AI PromptTemplate | 支持变量替换，灵活配置 |
| **优先级机制** | priority字段排序 | 控制角色加载顺序 |
| **标签分类** | tags字段 + 索引 | 便于检索和筛选 |
| **启用/禁用** | enabled字段 | 灵活控制角色可用性 |
| **多租户支持** | 可扩展tenant_id字段 | 支持SaaS化部署 |

---

## 五、项目架构设计

### 5.1 包结构

```
org.example
├── config/          # 配置类
│   └── ChatProperties.java
├── controller/      # REST API控制器
│   ├── ChatController.java
│   └── RoleConfigController.java
├── entity/          # 实体类
│   ├── ConversationSession.java
│   ├── ConversationMessage.java
│   └── RoleConfig.java
├── repository/      # 数据访问层
│   ├── ConversationSessionRepository.java
│   ├── ConversationMessageRepository.java
│   └── RoleConfigRepository.java
├── service/         # 业务逻辑层
│   ├── ChatService.java
│   └── RoleConfigService.java
├── exception/       # 异常处理
│   ├── ChatException.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
└── SpringAiJcStart.java  # 启动类
```

### 5.2 核心流程图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   客户端     │────▶│  ChatController │────▶│  ChatService    │
└─────────────┘     └─────────────┘     └─────────────────┘
                                                │
                          ┌─────────────────────┼─────────────────────┐
                          ▼                     ▼                     ▼
                   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
                   │   ChatClient │      │  Repository  │      │ RoleConfig  │
                   │  (Spring AI) │      │   (R2DBC)   │      │   Service   │
                   └─────────────┘      └─────────────┘      └─────────────┘
                          │                     │                     │
                          ▼                     ▼                     ▼
                   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
                   │  OpenAI API  │      │  PostgreSQL  │      │  角色配置表  │
                   └─────────────┘      └─────────────┘      └─────────────┘
```

---

## 六、代码实现详解

### 6.1 实体类设计

#### 会话实体 (ConversationSession)

```java
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
    // 工厂方法：创建新会话
    public static ConversationSession create(String title) {
        LocalDateTime now = LocalDateTime.now();
        return new ConversationSession(null, title, now, now);
    }
    
    // 更新会话时间
    public ConversationSession withUpdatedTime() {
        return new ConversationSession(
            this.id, this.title, this.createdAt, LocalDateTime.now()
        );
    }
}
```

**设计要点：**
- 使用 Java Record 简化实体定义
- 不可变设计，通过 `withUpdatedTime()` 创建新实例

#### 消息实体 (ConversationMessage)

```java
@Table("conversation_messages")
public record ConversationMessage(
    @Id Long id,
    @Column("session_id") Long sessionId,
    @Column("role") String role,        // user/assistant/system
    @Column("content") String content,
    @Column("created_at") LocalDateTime createdAt
) {
    public static ConversationMessage of(Long sessionId, String role, String content) {
        return new ConversationMessage(null, sessionId, role, content, LocalDateTime.now());
    }
}
```

### 5.2 配置属性类

```java
@Configuration
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    // 系统提示词 - 定义AI助手的行为准则
    private String systemPrompt = "你是一个友好、专业的AI助手...";

    // 最大保留的对话轮数（一对 = user + assistant）
    private int maxContextPairs = 20;

    // 计算实际消息条数
    public int getMaxContextMessages() {
        return maxContextPairs * 2;
    }
    
    // Getters and Setters...
}
```

**为什么使用外部化配置？**
- 无需修改代码即可调整AI行为
- 支持不同环境使用不同配置
- 便于运维人员管理

### 6.3 核心业务逻辑 (ChatService)

#### 6.3.1 普通聊天（非流式）

```java
public Mono<String> chat(Long sessionId, String userMessage, Long roleId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
        }
        
        return validateSessionExists(sessionId)
                .flatMap(sid -> buildConversationHistory(sid, roleId)
                        .flatMap(history -> {
                            // 添加用户消息到历史
                            history.add(new UserMessage(userMessage));
                            
                            Prompt prompt = new Prompt(new ArrayList<>(history));
                            
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
                            .onErrorResume(TransientAiException.class, e -> {
                                String errorMsg = extractErrorMessage(e);
                                return Mono.just("【AI服务暂时不可用】" + errorMsg);
                            })
                            .onErrorResume(Exception.class, e -> 
                                Mono.just("【请求失败】" + e.getMessage())
                            );
                        })
                );
    }
```

**关键代码解析：**

| 代码 | 作用 |
|------|------|
| `validateSessionExists` | 验证会话存在性，提前失败 |
| `buildConversationHistory` | 构建包含历史消息的对话上下文 |
| `subscribeOn(Schedulers.boundedElastic())` | 将阻塞的AI调用放到独立线程池 |
| `onErrorResume` | 错误恢复，避免服务崩溃 |

#### 6.3.2 流式聊天（SSE）

```java
public Flux<String> chatStream(Long sessionId, String userMessage, Long roleId) {
        if (sessionId == null) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空，请先创建会话"));
        }
        
        return validateSessionExists(sessionId)
                .flatMapMany(sid -> buildConversationHistory(sid, roleId)
                        .flatMapMany(history -> {
                            // 添加用户消息到历史
                            history.add(new UserMessage(userMessage));
                            
                            Prompt prompt = new Prompt(new ArrayList<>(history));
                            StringBuilder fullResponse = new StringBuilder();
                            
                            return chatClient.prompt(prompt)
                                    .stream()
                                    .content()
                                    .doOnNext(fullResponse::append)
                                    .doOnComplete(() -> {
                                        // 流完成后保存消息
                                        if (!fullResponse.isEmpty()) {
                                            saveMessage(sid, MessageType.USER.getValue(), userMessage)
                                                    .then(saveMessage(sid, MessageType.ASSISTANT.getValue(), fullResponse.toString()))
                                                    .subscribeOn(Schedulers.boundedElastic())
                                                    .subscribe();
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

**流式响应的优势：**
- 用户无需等待完整响应，提升体验
- 适合长文本生成场景
- 前端可实时展示打字效果

#### 6.3.3 构建对话历史（核心逻辑）

```java
private Mono<List<Message>> buildConversationHistory(Long sessionId, Long roleId) {
        return buildDynamicRoleMessages(roleId)
                .flatMap(dynamicMessages -> 
                        messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                                .collectList()
                                .map(messages -> {
                                    List<Message> history = new ArrayList<>();
                                    
                                    // 1. 添加动态角色配置生成的消息
                                    history.addAll(dynamicMessages);
                                    
                                    // 2. 处理历史消息（上下文截断）
                                    List<ConversationMessage> contextMessages = messages;
                                    int maxContextMessages = chatProperties.getMaxContextMessages();
                                    
                                    // 如果消息过多，只保留最近的 maxContextMessages 条
                                    if (messages.size() > maxContextMessages) {
                                        // 保留后 maxContextMessages 条
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
                                })
                );
    }
```

### 6.4 REST API 设计

#### 6.4.1 聊天控制器 (ChatController)

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 普通聊天（非流式）
     * @param request 包含 sessionId、message 和可选的 roleId
     * @return AI 回复
     */
    @PostMapping
    public Mono<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.sessionId(), request.message(), request.roleId());
    }

    /**
     * 流式聊天（SSE）
     * @param request 包含 sessionId、message 和可选的 roleId
     * @return 流式 AI 回复
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.sessionId(), request.message(), request.roleId());
    }

    // ==================== 会话管理 API ====================

    /**
     * 创建新会话
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
     * @return 按更新时间倒序排列的会话列表
     */
    @GetMapping("/sessions")
    public Flux<ConversationSession> getAllSessions() {
        return chatService.getAllSessions();
    }

    /**
     * 获取指定会话的详细信息
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ConversationSession> getSession(@PathVariable Long sessionId) {
        return chatService.getSession(sessionId);
    }

    /**
     * 获取指定会话的所有消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Mono<List<ConversationMessage>> getSessionMessages(@PathVariable Long sessionId) {
        return chatService.getSessionMessages(sessionId).collectList();
    }

    /**
     * 删除会话及其所有消息
     * @param sessionId 要删除的会话ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<Void> deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId);
    }

    // ==================== 请求/响应记录 ====================

    /**
     * 聊天请求
     * @param sessionId 会话ID（必填）
     * @param message 用户消息
     * @param roleId 选择的角色ID（可选，如果为空则使用默认角色）
     */
    public record ChatRequest(Long sessionId, String message, Long roleId) {}

    /**
     * 创建会话请求
     * @param title 会话标题（可选）
     */
    public record CreateSessionRequest(String title) {}

    /**
     * 创建会话响应
     * @param sessionId 新创建的会话ID
     */
    public record CreateSessionResponse(Long sessionId) {}
}
```

#### 6.4.2 角色配置控制器 (RoleConfigController)

```java
@RestController
@RequestMapping("/api/role-configs")
public class RoleConfigController {

    private final RoleConfigService roleConfigService;

    public RoleConfigController(RoleConfigService roleConfigService) {
        this.roleConfigService = roleConfigService;
    }

    /**
     * 创建新的角色配置
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoleConfig> createRoleConfig(@RequestBody CreateRoleConfigRequest request) {
        return roleConfigService.createRoleConfig(
                request.roleType(),
                request.roleName(),
                request.description(),
                request.templateContent(),
                request.enabled(),
                request.priority(),
                request.tags()
        );
    }

    /**
     * 更新角色配置
     */
    @PutMapping("/{id}")
    public Mono<RoleConfig> updateRoleConfig(
            @PathVariable Long id,
            @RequestBody UpdateRoleConfigRequest request
    ) {
        return roleConfigService.updateRoleConfig(
                id,
                request.roleName(),
                request.description(),
                request.templateContent(),
                request.enabled(),
                request.priority(),
                request.tags()
        );
    }

    /**
     * 删除角色配置
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRoleConfig(@PathVariable Long id) {
        return roleConfigService.deleteRoleConfig(id);
    }

    /**
     * 根据ID获取角色配置
     */
    @GetMapping("/{id}")
    public Mono<RoleConfig> getRoleConfigById(@PathVariable Long id) {
        return roleConfigService.getRoleConfigById(id);
    }

    /**
     * 获取所有角色配置
     */
    @GetMapping
    public Flux<RoleConfig> getAllRoleConfigs() {
        return roleConfigService.getAllRoleConfigs();
    }

    /**
     * 获取所有启用的角色配置
     */
    @GetMapping("/enabled")
    public Flux<RoleConfig> getAllEnabledRoleConfigs() {
        return roleConfigService.getAllEnabledRoleConfigs();
    }

    /**
     * 根据角色类型获取启用的配置
     */
    @GetMapping("/type/{roleType}")
    public Flux<RoleConfig> getEnabledRoleConfigsByType(@PathVariable String roleType) {
        return roleConfigService.getEnabledRoleConfigsByType(roleType);
    }

    /**
     * 渲染角色模板
     */
    @PostMapping("/{id}/render")
    public Mono<String> renderRoleTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> variables
    ) {
        return roleConfigService.getRoleConfigById(id)
                .flatMap(roleConfig -> roleConfigService.renderRoleTemplate(roleConfig, variables));
    }

    // ==================== 请求记录定义 ====================

    public record CreateRoleConfigRequest(
            String roleType,
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {}

    public record UpdateRoleConfigRequest(
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {}
}
```

---

## 七、API使用指南

### 7.1 快速开始

#### 1. 创建会话

```bash
curl -X POST http://localhost:8080/api/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "技术咨询会话"}'
```

响应：
```json
{"sessionId": 1}
```

#### 2. 发送消息（非流式）

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "解释一下Java中的Mono和Flux"
  }'
```

#### 3. 流式聊天（SSE）

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "写一个快速排序算法"
  }'
```

**前端SSE接收示例：**
```javascript
const eventSource = new EventSource('/api/chat/stream');
eventSource.onmessage = (event) => {
    console.log('收到:', event.data);
    // 实时更新UI
};
```

#### 4. 使用特定角色

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "message": "帮我优化这段代码",
    "roleId": 2  // 使用"技术专家"角色
  }'
```

### 7.2 完整API列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 普通聊天 |
| POST | `/api/chat/stream` | 流式聊天 |
| POST | `/api/chat/sessions` | 创建会话 |
| GET | `/api/chat/sessions` | 获取所有会话 |
| GET | `/api/chat/sessions/{id}` | 获取会话详情 |
| GET | `/api/chat/sessions/{id}/messages` | 获取会话消息 |
| DELETE | `/api/chat/sessions/{id}` | 删除会话 |

---

## 八、避坑指南与优化建议

### 8.1 常见问题

#### Q1: API调用超时

**现象：** 大模型响应慢，导致HTTP超时

**解决方案：**
```yaml
server:
  netty:
    connection-timeout: 60s  # 增加超时时间
```

#### Q2: Token超限

**现象：** 长对话时AI返回错误

**解决方案：**
- 调整 `max-context-pairs` 配置
- 实现更智能的上下文压缩策略
- 使用Token计数器精确控制

#### Q3: 数据库连接池耗尽

**现象：** 高并发时连接池耗尽

**解决方案：**
```yaml
spring:
  r2dbc:
    pool:
      max-size: 50        # 增加连接池大小
      max-idle-time: 30m  # 调整空闲时间
```

### 8.2 性能优化建议

1. **消息缓存**：对热点会话使用Redis缓存历史消息
2. **异步保存**：已实现，流式响应时异步保存消息
3. **连接复用**：使用HTTP/2或连接池复用AI API连接
4. **限流保护**：添加RateLimiter防止API滥用

### 8.3 安全建议

- **API密钥管理**：使用环境变量或密钥管理服务
- **输入校验**：防止Prompt注入攻击
- **权限控制**：为不同用户隔离会话数据
- **审计日志**：记录关键操作便于追溯

---

## 九、总结与扩展

### 9.1 项目核心回顾

本项目实现了以下核心能力：

1. **无状态设计**：不保存当前会话状态，所有操作通过sessionId显式指定
2. **上下文控制**：滑动窗口策略防止Token超限
3. **响应式架构**：全链路异步非阻塞，提升吞吐量
4. **动态角色**：支持多角色配置，灵活应对不同场景

### 9.2 可扩展方向

| 方向 | 实现思路 |
|------|----------|
| **多模型支持** | 集成Claude、Gemini等，通过配置切换 |
| **RAG增强** | 接入向量数据库，实现知识库问答 |
| **Function Calling** | 扩展Tool角色，支持调用外部API |
| **多模态** | 支持图片、语音输入输出 |
| **对话总结** | 长对话自动摘要，压缩上下文 |

### 9.3 参考资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Project Reactor 文档](https://projectreactor.io/docs)
- [R2DBC 规范](https://r2dbc.io/)

---

## 附录

### 技术标签

`Spring Boot` `Spring AI` `WebFlux` `R2DBC` `PostgreSQL` `响应式编程` `AI应用开发`

---

> 💡 **原创声明**：本文为原创教程，转载请注明出处。
> 
> 欢迎在评论区交流讨论！如有问题或建议，请留言反馈。
