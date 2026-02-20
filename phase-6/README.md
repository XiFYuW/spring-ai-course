# Spring AI 工具调用实战：手把手教你让 AI 拥有"超能力"

---

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-6](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-6)

## 引言

你是否曾想过，如果 ChatGPT 不仅能回答问题，还能**帮你设置闹钟、查询天气、进行复杂计算**，会是怎样的体验？这正是 **AI 工具调用（Function Calling）** 技术的魅力所在！

本文将带你深入探索 **Spring AI 的工具调用功能**，通过一个完整的实战项目，手把手教你如何让 AI "学会"使用各种工具，从而突破大模型知识截止日期的限制，实现与外部世界的实时交互。

**读完本文，你将收获：**
- 深入理解 AI 工具调用的核心原理
- 掌握 Spring AI 中 `@Tool` 注解的使用方法
- 学会构建多工具组合调用的智能应用
- 获得一套可直接运行的完整代码示例

---

## 目录

- [一、什么是 AI 工具调用？](#一什么是-ai-工具调用)
- [二、项目概述与技术栈](#二项目概述与技术栈)
- [三、环境准备](#三环境准备)
- [四、核心概念详解](#四核心概念详解)
- [五、实战步骤拆解](#五实战步骤拆解)
  - [5.1 创建 Spring Boot 项目](#51-创建-spring-boot-项目)
  - [5.2 配置 AI 模型](#52-配置-ai-模型)
  - [5.3 开发第一个工具：日期时间工具](#53-开发第一个工具日期时间工具)
  - [5.4 开发信息检索工具：天气查询](#54-开发信息检索工具天气查询)
  - [5.5 开发数学计算工具](#55-开发数学计算工具)
  - [5.6 开发操作执行工具：智能闹钟](#56-开发操作执行工具智能闹钟)
  - [5.7 构建 RESTful API 控制器](#57-构建-restful-api-控制器)
- [六、多工具组合调用演示](#六多工具组合调用演示)
- [七、流式响应实现](#七流式响应实现)
- [八、避坑指南与最佳实践](#八避坑指南与最佳实践)
- [九、效果展示](#九效果展示)
- [十、总结与扩展思考](#十总结与扩展思考)

---

## 一、什么是 AI 工具调用？

### 1.1 核心概念

**工具调用（Tool Calling / Function Calling）** 是一种让大语言模型（LLM）能够调用外部函数或 API 的技术。通过这种方式，AI 可以：

- **获取实时信息**：查询当前时间、天气、股票价格等
- **执行具体操作**：发送邮件、设置提醒、操作数据库
- **进行复杂计算**：解决数学问题、数据分析
- **与外部系统集成**：调用第三方 API、操作硬件设备

### 1.2 工作原理

```
┌─────────────┐     用户提问      ┌─────────────┐
│   用户      │ ───────────────> │  大语言模型  │
└─────────────┘                  └──────┬──────┘
                                        │
                                        │ 识别需要调用工具
                                        ▼
                               ┌─────────────────┐
                               │  生成工具调用请求  │
                               │  (函数名+参数)    │
                               └────────┬────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │   执行工具方法    │
                               │  (Java 方法调用)  │
                               └────────┬────────┘
                                        │
                                        │ 返回执行结果
                                        ▼
                               ┌─────────────────┐
                               │  生成最终回复    │
                               └────────┬────────┘
                                        │
┌─────────────┐     AI 回复       ┌────┴──────┐
│   用户      │ <───────────────  │  大语言模型 │
└─────────────┘                   └───────────┘
```

### 1.3 为什么需要工具调用？

大语言模型虽然强大，但存在以下局限：

| 局限 | 说明 | 工具调用解决方案 |
|------|------|-----------------|
| 知识截止日期 | 模型训练数据有截止时间 | 调用实时 API 获取最新信息 |
| 无法执行操作 | 只能生成文本，不能实际操作 | 调用执行类工具完成操作 |
| 数学计算能力有限 | 复杂计算容易出错 | 调用计算器工具精确计算 |
| 无法访问私有数据 | 不知道用户个人信息 | 调用数据库查询工具 |

---

## 二、项目概述与技术栈

### 2.1 项目结构

```
spring-ai-jc/
├── src/main/java/org/example/
│   ├── controller/
│   │   └── ToolController.java          # REST API 控制器
│   ├── exception/
│   │   ├── ChatException.java           # 自定义异常
│   │   ├── ErrorResponse.java           # 错误响应体
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── tools/
│   │   ├── DateTimeTools.java           # 日期时间工具
│   │   ├── WeatherTools.java            # 天气查询工具
│   │   ├── CalculatorTools.java         # 计算器工具
│   │   └── AlarmTools.java              # 闹钟提醒工具
│   └── SpringAiJcStart.java             # 启动类
├── src/main/resources/
│   └── application.yml                  # 配置文件
└── pom.xml                              # Maven 依赖
```

### 2.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.1.0-SNAPSHOT | AI 工具调用核心库 |
| OpenAI API | - | 大语言模型服务 |
| Java | 25 | 开发语言 |
| Maven | - | 构建工具 |

### 2.3 实现的功能

本项目实现了 **4 大类工具**，覆盖不同的应用场景：

1. **信息检索类**：日期时间查询、天气查询
2. **数学计算类**：基础运算、高精度计算、科学计算
3. **操作执行类**：设置闹钟、取消提醒
4. **组合调用**：多工具协同工作

---

## 三、环境准备

### 3.1 前置要求

- **JDK 25** 或更高版本
- **Maven 3.8+**
- **OpenAI API Key**（或其他兼容的 AI 服务）

### 3.2 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的 `https://ai.32zi.com`）

**配置方式**：在 `application.yml` 中设置你的 API Key

### 3.3 克隆项目并运行

```bash
# 克隆项目
git clone <repository-url>
cd spring-ai-jc

# 编译运行
mvn spring-boot:run
```

---

## 四、核心概念详解

### 4.1 @Tool 注解

Spring AI 提供了 `@Tool` 注解，用于标记一个方法作为 AI 可调用的工具：

```java
@Tool(description = "获取当前日期和时间")
public String getCurrentDateTime() {
    // 工具实现
}
```

**关键属性**：
- `description`：**必需**，详细描述工具的功能，帮助 AI 理解何时使用该工具

### 4.2 @ToolParam 注解

用于标记工具方法的参数：

```java
@Tool(description = "计算两个日期的天数差")
public long calculateDaysBetween(
    @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
    @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate
) {
    // 实现
}
```

**关键属性**：
- `description`：描述参数的含义和格式
- `required`：是否为必需参数（默认 true）

### 4.3 ChatClient 工具调用

在控制器中使用 `ChatClient` 进行工具调用：

```java
String response = chatClient.prompt()
    .system("系统提示词")           // 设置系统角色
    .user("用户问题")              // 用户输入
    .tools(dateTimeTools)          // 注册可用工具
    .call()                        // 执行调用
    .content();                    // 获取回复内容
```

---

## 五、实战步骤拆解

### 5.1 创建 Spring Boot 项目

**pom.xml 核心依赖**：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
</parent>

<dependencies>
    <!-- WebFlux 响应式编程 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Spring AI OpenAI 启动器 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

### 5.2 配置 AI 模型

**application.yml**：

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-api-key-here
      base-url: https://ai.32zi.com  # 可替换为你的 API 端点
      chat:
        options:
          model: claude-3-7-sonnet-20250219  # 或其他支持的模型
      timeout:
        connect: 30s
        read: 120s
```

### 5.3 开发第一个工具：日期时间工具

**DateTimeTools.java**：

```java
package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTools {

    /**
     * 获取当前日期和时间
     */
    @Tool(description = "获取用户所在时区的当前日期和时间，格式为 ISO-8601")
    public String getCurrentDateTime() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String result = now.format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("[工具调用] getCurrentDateTime() = " + result);
        return result;
    }

    /**
     * 获取当前日期
     */
    @Tool(description = "获取用户所在时区的当前日期，格式为 yyyy-MM-dd")
    public String getCurrentDate() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取当前时间
     */
    @Tool(description = "获取用户所在时区的当前时间，格式为 HH:mm:ss")
    public String getCurrentTime() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        return now.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    /**
     * 获取当前星期几
     */
    @Tool(description = "获取今天是星期几（中文）")
    public String getDayOfWeek() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String[] days = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        int dayIndex = now.getDayOfWeek().getValue() - 1;
        return days[dayIndex];
    }

    /**
     * 计算两个日期之间的天数差
     */
    @Tool(description = "计算两个日期之间的天数差，日期格式为 yyyy-MM-dd")
    public long calculateDaysBetween(
            @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T00:00:00");
            return java.time.Duration.between(start, end).toDays();
        } catch (DateTimeParseException e) {
            System.err.println("[工具调用错误] 日期解析失败: " + e.getMessage());
            return -1;
        }
    }
}
```

**要点解析**：
- 使用 `@Component` 将工具类纳入 Spring 管理
- `@Tool` 的 `description` 要清晰描述功能，这是 AI 选择工具的关键依据
- `@ToolParam` 说明参数格式，帮助 AI 正确传参

### 5.4 开发信息检索工具：天气查询

**WeatherTools.java**（模拟天气 API 调用）：

```java
package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class WeatherTools {

    private final Random random = new Random();
    private final Map<String, WeatherData> weatherCache = new HashMap<>();

    /**
     * 获取指定城市的当前天气
     */
    @Tool(description = "获取指定城市的当前天气信息，包括温度、天气状况、湿度、风速等")
    public String getCurrentWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        
        // 模拟 API 调用延迟
        simulateApiDelay();
        
        // 生成模拟天气数据（实际应用中调用真实天气 API）
        WeatherData data = generateMockWeather(city);
        weatherCache.put(city, data);
        
        String result = formatWeatherData(data);
        System.out.println("[工具调用] getCurrentWeather(" + city + ") -> 数据已获取");
        return result;
    }

    /**
     * 获取天气预报
     */
    @Tool(description = "获取指定城市未来几天的天气预报")
    public String getWeatherForecast(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city,
            @ToolParam(description = "预报天数（1-7天）", required = false) Integer days) {
        
        int forecastDays = days != null ? Math.min(Math.max(days, 1), 7) : 3;
        simulateApiDelay();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🌤️ %s 未来 %d 天天气预报\n", city, forecastDays));
        // ... 生成预报数据
        return sb.toString();
    }

    private WeatherData generateMockWeather(String city) {
        String[] conditions = {"晴", "多云", "阴", "小雨", "中雨"};
        String condition = conditions[random.nextInt(conditions.length)];
        double temperature = 15 + random.nextInt(20);
        int humidity = 40 + random.nextInt(40);
        
        return new WeatherData(city, condition, temperature, humidity, 
            5 + random.nextInt(15),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private void simulateApiDelay() {
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class WeatherData {
        String city, condition, updateTime;
        double temperature, windSpeed;
        int humidity;
        
        WeatherData(String city, String condition, double temperature, 
                    int humidity, double windSpeed, String updateTime) {
            this.city = city;
            this.condition = condition;
            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.updateTime = updateTime;
        }
    }
}
```

**实际应用建议**：
- 接入真实天气 API（如和风天气、OpenWeatherMap）
- 添加缓存机制避免频繁调用
- 实现错误重试和降级策略

### 5.5 开发数学计算工具

**CalculatorTools.java**：

```java
package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculatorTools {

    private static final int DEFAULT_SCALE = 10;

    @Tool(description = "计算两个数字的和")
    public double add(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        double result = a + b;
        System.out.printf("[工具调用] add(%.2f, %.2f) = %.2f%n", a, b, result);
        return result;
    }

    @Tool(description = "计算两个数字的差（第一个数减去第二个数）")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b) {
        return a - b;
    }

    @Tool(description = "计算两个数字的乘积")
    public double multiply(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        return a * b;
    }

    @Tool(description = "计算两个数字的商（第一个数除以第二个数）")
    public double divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数") double b) {
        if (b == 0) {
            throw new ArithmeticException("除数不能为 0");
        }
        return a / b;
    }

    /**
     * 高精度加法（适用于金融计算）
     */
    @Tool(description = "高精度计算两个数字的和，适用于金融计算")
    public String addPrecise(
            @ToolParam(description = "第一个数字（字符串格式）") String a,
            @ToolParam(description = "第二个数字（字符串格式）") String b,
            @ToolParam(description = "小数位数", required = false) Integer scale) {
        
        try {
            BigDecimal num1 = new BigDecimal(a);
            BigDecimal num2 = new BigDecimal(b);
            int precision = scale != null ? scale : 2;
            
            BigDecimal result = num1.add(num2).setScale(precision, RoundingMode.HALF_UP);
            return result.toPlainString();
        } catch (NumberFormatException e) {
            return "错误：数字格式不正确 - " + e.getMessage();
        }
    }

    @Tool(description = "计算一个数的幂运算")
    public double power(
            @ToolParam(description = "底数") double base,
            @ToolParam(description = "指数") double exponent) {
        return Math.pow(base, exponent);
    }

    @Tool(description = "计算一个数的平方根")
    public double sqrt(
            @ToolParam(description = "被开方数") double value) {
        if (value < 0) {
            throw new IllegalArgumentException("不能对负数开平方");
        }
        return Math.sqrt(value);
    }
}
```

### 5.6 开发操作执行工具：智能闹钟

**AlarmTools.java**：

```java
package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AlarmTools {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Map<String, AlarmInfo> alarms = new ConcurrentHashMap<>();
    private int alarmCounter = 0;

    private static class AlarmInfo {
        final String id, time, message;
        final boolean isRecurring;
        final long createdAt;

        AlarmInfo(String id, String time, String message, boolean isRecurring) {
            this.id = id;
            this.time = time;
            this.message = message;
            this.isRecurring = isRecurring;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * 设置指定时间的闹钟
     */
    @Tool(description = "在指定时间设置闹钟提醒，时间格式为 ISO-8601 (yyyy-MM-ddTHH:mm:ss)")
    public String setAlarm(
            @ToolParam(description = "闹钟时间，ISO-8601 格式") String time,
            @ToolParam(description = "提醒消息内容") String message,
            @ToolParam(description = "是否重复提醒", required = false) Boolean isRecurring) {
        
        try {
            LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            
            if (alarmTime.isBefore(now)) {
                return "错误：设置的时间 " + time + " 已经过去";
            }
            
            String alarmId = "ALARM_" + (++alarmCounter);
            boolean recurring = isRecurring != null && isRecurring;
            long delaySeconds = java.time.Duration.between(now, alarmTime).getSeconds();
            
            AlarmInfo alarmInfo = new AlarmInfo(alarmId, time, message, recurring);
            alarms.put(alarmId, alarmInfo);
            
            scheduler.schedule(() -> triggerAlarm(alarmId, message), 
                delaySeconds, TimeUnit.SECONDS);
            
            return String.format(
                "✅ 闹钟设置成功！\nID: %s\n时间: %s\n消息: %s\n将在 %d 秒后触发",
                alarmId, time, message, delaySeconds
            );
            
        } catch (DateTimeParseException e) {
            return "错误：时间格式不正确。请使用 ISO-8601 格式";
        }
    }

    /**
     * 设置相对时间闹钟（几分钟后）
     */
    @Tool(description = "设置从现在开始多少分钟后的闹钟")
    public String setAlarmInMinutes(
            @ToolParam(description = "多少分钟后") int minutes,
            @ToolParam(description = "提醒消息") String message) {
        
        LocalDateTime alarmTime = LocalDateTime.now().plusMinutes(minutes);
        return setAlarm(alarmTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), message, false);
    }

    /**
     * 列出所有闹钟
     */
    @Tool(description = "列出所有已设置的闹钟")
    public String listAlarms() {
        if (alarms.isEmpty()) {
            return "当前没有设置的闹钟";
        }
        
        StringBuilder sb = new StringBuilder("⏰ 已设置的闹钟列表：\n");
        alarms.forEach((id, alarm) -> {
            sb.append(String.format("  %s - %s: %s\n", id, alarm.time, alarm.message));
        });
        return sb.toString();
    }

    /**
     * 取消指定闹钟
     */
    @Tool(description = "根据ID取消指定的闹钟")
    public String cancelAlarm(
            @ToolParam(description = "闹钟ID") String alarmId) {
        AlarmInfo removed = alarms.remove(alarmId);
        if (removed != null) {
            return "✅ 已取消闹钟: " + alarmId;
        }
        return "❌ 未找到闹钟: " + alarmId;
    }

    private void triggerAlarm(String alarmId, String message) {
        System.out.println("\n🔔 闹钟触发！");
        System.out.println("   ID: " + alarmId);
        System.out.println("   消息: " + message);
        System.out.println("   时间: " + LocalDateTime.now());
        alarms.remove(alarmId);
    }
}
```

### 5.7 构建 RESTful API 控制器

**ToolController.java**：

```java
package org.example.controller;

import org.example.tools.AlarmTools;
import org.example.tools.CalculatorTools;
import org.example.tools.DateTimeTools;
import org.example.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ChatClient chatClient;
    private final DateTimeTools dateTimeTools;
    private final AlarmTools alarmTools;
    private final CalculatorTools calculatorTools;
    private final WeatherTools weatherTools;

    public ToolController(ChatClient.Builder chatClientBuilder,
                          DateTimeTools dateTimeTools,
                          AlarmTools alarmTools,
                          CalculatorTools calculatorTools,
                          WeatherTools weatherTools) {
        this.chatClient = chatClientBuilder.build();
        this.dateTimeTools = dateTimeTools;
        this.alarmTools = alarmTools;
        this.calculatorTools = calculatorTools;
        this.weatherTools = weatherTools;
    }

    /**
     * 日期时间查询接口
     */
    @GetMapping("/datetime")
    public Mono<String> dateTimeDemo(
            @RequestParam(defaultValue = "今天是几号？") String question) {
        return Mono.fromCallable(() -> {
            return chatClient.prompt()
                    .system("你是一个 helpful 的助手。当用户询问日期、时间相关问题时，请使用提供的工具获取准确信息。")
                    .user(question)
                    .tools(dateTimeTools)
                    .call()
                    .content();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 闹钟设置接口
     */
    @PostMapping("/alarm")
    public Mono<String> alarmDemo(@RequestBody AlarmRequest request) {
        return Mono.fromCallable(() -> {
            return chatClient.prompt()
                    .system("""
                        你是一个智能闹钟助手。你可以帮用户设置、取消、查询闹钟。
                        可用工具说明：
                        - setAlarm: 设置指定时间的闹钟
                        - setAlarmInMinutes: 设置从现在开始多少分钟后的闹钟
                        - cancelAlarm: 根据ID取消闹钟
                        - listAlarms: 列出所有闹钟
                        """)
                    .user(request.command())
                    .tools(alarmTools)
                    .call()
                    .content();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 计算器接口
     */
    @PostMapping("/calculator")
    public Mono<String> calculatorDemo(@RequestBody CalculatorRequest request) {
        return Mono.fromCallable(() -> {
            return chatClient.prompt()
                    .system("你是一个数学计算助手。当用户需要进行数学计算时，请使用提供的计算工具。")
                    .user(request.question())
                    .tools(calculatorTools)
                    .call()
                    .content();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 天气查询接口
     */
    @GetMapping("/weather")
    public Mono<String> weatherDemo(
            @RequestParam(defaultValue = "北京") String city,
            @RequestParam(defaultValue = "获取当前天气") String query) {
        return Mono.fromCallable(() -> {
            return chatClient.prompt()
                    .system("你是一个天气助手。你可以帮用户查询天气信息。")
                    .user(query + "，城市是：" + city)
                    .tools(weatherTools)
                    .call()
                    .content();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // 请求记录类
    public record AlarmRequest(String command) {}
    public record CalculatorRequest(String question) {}
}
```

---

## 六、多工具组合调用演示

**多工具协同**是工具调用的高级用法，AI 可以根据需求自动选择和组合多个工具：

```java
@PostMapping("/multi-tools")
public Mono<String> multiToolsDemo(@RequestBody MultiToolsRequest request) {
    return Mono.fromCallable(() -> {
        String response = chatClient.prompt()
                .system("""
                    你是一个智能助手，可以使用多种工具来帮助用户。
                    你可以同时使用以下工具：
                    - 日期时间工具：获取当前时间、计算日期差等
                    - 闹钟工具：设置提醒
                    - 计算器工具：进行数学计算
                    - 天气工具：查询天气信息
                    
                    请根据用户的需求，灵活组合使用这些工具。
                    """)
                .user(request.question())
                .tools(dateTimeTools, alarmTools, calculatorTools, weatherTools)  // 同时注册多个工具
                .call()
                .content();
        return response;
    }).subscribeOn(Schedulers.boundedElastic());
}
```

**示例场景**：

用户提问：*"现在几点了？帮我计算 123 乘以 456 等于多少？顺便查一下北京今天的天气。"*

AI 会：
1. 调用 `getCurrentTime()` 获取当前时间
2. 调用 `multiply(123, 456)` 进行计算
3. 调用 `getCurrentWeather("北京")` 查询天气
4. 整合所有结果，给出完整回复

---

## 七、流式响应实现

对于需要实时反馈的场景，可以实现 **SSE（Server-Sent Events）** 流式响应：

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamToolDemo(
        @RequestParam(defaultValue = "现在几点了？") String question) {
    
    return chatClient.prompt()
            .system("你是一个 helpful 的助手，可以使用日期时间和计算工具。")
            .user(question)
            .tools(dateTimeTools, calculatorTools)
            .stream()  // 启用流式输出
            .content()
            .doOnNext(chunk -> System.out.print(chunk))  // 实时打印
            .doOnComplete(() -> System.out.println("\n[流式输出完成]"));
}
```

**前端调用示例**：

```javascript
const eventSource = new EventSource('/api/tools/stream?question=现在几点了？');

eventSource.onmessage = (event) => {
    console.log('收到数据:', event.data);
    document.getElementById('output').innerHTML += event.data;
};

eventSource.onerror = (error) => {
    console.error('SSE 错误:', error);
    eventSource.close();
};
```

---

## 八、避坑指南与最佳实践

### 8.1 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| AI 不调用工具 | 工具描述不清晰 | 优化 `@Tool` 的 description，明确说明使用场景 |
| 参数传递错误 | 参数描述不明确 | 使用 `@ToolParam` 详细说明参数格式和示例 |
| 工具调用超时 | 方法执行太慢 | 优化工具实现，或增加超时配置 |
| 并发问题 | 工具类非线程安全 | 确保工具方法无副作用，或使用同步机制 |

### 8.2 最佳实践

1. **工具描述要具体**
   ```java
   // ❌ 不好的描述
   @Tool(description = "获取天气")
   
   // ✅ 好的描述
   @Tool(description = "获取指定城市的当前天气信息，包括温度、天气状况、湿度、风速等。参数city为城市名称，如：北京、上海")
   ```

2. **参数说明要完整**
   ```java
   @ToolParam(description = "开始日期，格式 yyyy-MM-dd，例如：2024-01-15")
   ```

3. **添加日志记录**
   ```java
   System.out.println("[工具调用] 方法名(参数) = 结果");
   ```

4. **错误处理要完善**
   ```java
   try {
       // 工具逻辑
   } catch (SpecificException e) {
       return "错误：具体错误信息";
   }
   ```

5. **使用响应式编程**
   ```java
   return Mono.fromCallable(() -> {
       // 阻塞操作
   }).subscribeOn(Schedulers.boundedElastic());
   ```

---

## 九、效果展示

### 9.1 日期时间查询

**请求**：
```bash
curl "http://localhost:8080/api/tools/datetime?question=今天是几号？明天是几号？"
```

**控制台输出**：
```
========== 日期时间工具调用演示 ==========
用户问题: 今天是几号？明天是几号？
----------------------------------------
[工具调用] getCurrentDate() = 2026-02-20
AI 回答: 今天是2026年2月20日，明天是2月21日。
========================================
```

### 9.2 闹钟设置

**请求**：
```bash
curl -X POST "http://localhost:8080/api/tools/alarm" \
  -H "Content-Type: application/json" \
  -d '{"command": "帮我设置一个明天早上8点的闹钟，提醒我去开会"}'
```

**响应**：
```
✅ 闹钟设置成功！
ID: ALARM_1
时间: 2026-02-21T08:00:00
消息: 提醒我去开会
将在 43200 秒后触发
```

### 9.3 多工具组合

**请求**：
```bash
curl -X POST "http://localhost:8080/api/tools/multi-tools" \
  -H "Content-Type: application/json" \
  -d '{"question": "现在几点了？北京天气如何？计算 100 除以 4"}'
```

**AI 执行过程**：
1. 调用 `getCurrentTime()` 获取时间
2. 调用 `getCurrentWeather("北京")` 查询天气
3. 调用 `divide(100, 4)` 进行计算
4. 整合回复

---

## 十、总结与扩展思考

### 10.1 核心要点回顾

本文通过实战项目，系统讲解了 Spring AI 工具调用的完整流程：

1. **@Tool 注解**：标记可调用方法
2. **@ToolParam 注解**：描述参数信息
3. **ChatClient**：配置系统提示词、注册用户工具、执行调用
4. **多工具组合**：让 AI 灵活选择和使用多个工具

### 10.2 可以扩展的功能

基于本项目，你可以进一步实现：

- **数据库操作工具**：让 AI 帮你查询和修改数据库
- **邮件发送工具**：让 AI 帮你发送邮件
- **文件处理工具**：让 AI 读写文件、生成报表
- **第三方 API 集成**：接入更多外部服务（地图、翻译、OCR 等）
- **记忆功能**：让 AI 记住用户的偏好和历史对话

### 10.3 性能优化方向

- **工具缓存**：缓存常用工具调用结果
- **异步执行**：使用 CompletableFuture 并行执行多个工具
- **工具链**：实现工具之间的依赖和流水线处理
- **权限控制**：为不同工具设置访问权限

---

## 附录

### 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [OpenAI Function Calling 指南](https://platform.openai.com/docs/guides/function-calling)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)

---

## 💰 为什么选择 32ai？

**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。

---

<!-- 互动提示 -->
欢迎在评论区交流讨论！如果你有任何问题或建议，欢迎留言。

**原创声明**：本文为原创教程，转载请注明出处。

---
