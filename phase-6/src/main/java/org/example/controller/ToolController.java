package org.example.controller;

import org.example.tools.AlarmTools;
import org.example.tools.CalculatorTools;
import org.example.tools.DateTimeTools;
import org.example.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 工具调用演示控制器
 * 演示 Spring AI 的工具调用功能
 */
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
     * 基础工具调用演示 - 获取当前时间
     * 演示如何让 AI 使用工具获取实时信息
     */
    @GetMapping("/datetime")
    public Mono<String> dateTimeDemo(@RequestParam(defaultValue = "今天是几号？明天是几号？") String question) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== 日期时间工具调用演示 ==========");
            System.out.println("用户问题: " + question);
            System.out.println("----------------------------------------");

            String response = chatClient.prompt()
                    .system("你是一个 helpful 的助手。当用户询问日期、时间相关问题时，请使用提供的工具获取准确信息。")
                    .user(question)
                    .tools(dateTimeTools)
                    .call()
                    .content();

            System.out.println("AI 回答: " + response);
            System.out.println("========================================\n");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 闹钟工具调用演示
     * 演示如何让 AI 执行操作类工具
     */
    @PostMapping("/alarm")
    public Mono<String> alarmDemo(@RequestBody AlarmRequest request) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== 闹钟工具调用演示 ==========");
            System.out.println("用户指令: " + request.command());
            System.out.println("----------------------------------------");

            String response = chatClient.prompt()
                    .system("""
                        你是一个智能闹钟助手。你可以帮用户设置、取消、查询闹钟。
                        可用工具说明：
                        - setAlarm: 设置指定时间的闹钟，时间格式为 ISO-8601 (yyyy-MM-ddTHH:mm:ss)
                        - setAlarmInMinutes: 设置从现在开始多少分钟后的闹钟
                        - cancelAlarm: 根据ID取消闹钟
                        - listAlarms: 列出所有闹钟
                        - clearAllAlarms: 清除所有闹钟
                        
                        设置闹钟时，请向用户确认设置详情。
                        """)
                    .user(request.command())
                    .tools(alarmTools)
                    .call()
                    .content();

            System.out.println("AI 回答: " + response);
            System.out.println("======================================\n");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 计算器工具调用演示
     */
    @PostMapping("/calculator")
    public Mono<String> calculatorDemo(@RequestBody CalculatorRequest request) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== 计算器工具调用演示 ==========");
            System.out.println("用户问题: " + request.question());
            System.out.println("----------------------------------------");

            String response = chatClient.prompt()
                    .system("""
                        你是一个数学计算助手。当用户需要进行数学计算时，请使用提供的计算工具。
                        对于复杂的计算，请分步骤调用工具完成。
                        注意：对于高精度金融计算，请使用 addPrecise 方法。
                        """)
                    .user(request.question())
                    .tools(calculatorTools)
                    .call()
                    .content();

            System.out.println("AI 回答: " + response);
            System.out.println("=======================================\n");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 天气工具调用演示
     */
    @GetMapping("/weather")
    public Mono<String> weatherDemo(
            @RequestParam(defaultValue = "北京") String city,
            @RequestParam(defaultValue = "获取当前天气") String query) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== 天气工具调用演示 ==========");
            System.out.println("城市: " + city + ", 查询: " + query);
            System.out.println("----------------------------------------");

            String response = chatClient.prompt()
                    .system("""
                        你是一个天气助手。你可以帮用户查询天气信息。
                        可用工具：
                        - getCurrentWeather: 获取当前天气
                        - getWeatherForecast: 获取天气预报
                        - getAirQuality: 获取空气质量
                        - compareWeather: 比较两个城市的天气
                        """)
                    .user(query + "，城市是：" + city)
                    .tools(weatherTools)
                    .call()
                    .content();

            System.out.println("AI 回答: " + response);
            System.out.println("=====================================\n");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 多工具组合调用演示
     * 演示 AI 如何根据需求自动选择和使用多个工具
     */
    @PostMapping("/multi-tools")
    public Mono<String> multiToolsDemo(@RequestBody MultiToolsRequest request) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== 多工具组合调用演示 ==========");
            System.out.println("用户问题: " + request.question());
            System.out.println("----------------------------------------");

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
                    .tools(dateTimeTools, alarmTools, calculatorTools, weatherTools)
                    .call()
                    .content();

            System.out.println("AI 回答: " + response);
            System.out.println("======================================\n");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 流式响应 - 工具调用
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamToolDemo(
            @RequestParam(defaultValue = "现在几点了？帮我计算 123 乘以 456 等于多少？") String question) {
        System.out.println("\n========== 流式工具调用演示 ==========");
        System.out.println("用户问题: " + question);
        System.out.println("----------------------------------------");

        return chatClient.prompt()
                .system("你是一个 helpful 的助手，可以使用日期时间和计算工具。")
                .user(question)
                .tools(dateTimeTools, calculatorTools)
                .stream()
                .content()
                .doOnNext(chunk -> System.out.print(chunk))
                .doOnComplete(() -> System.out.println("\n======================================\n"));
    }

    // ========== 请求记录类 ==========

    public record AlarmRequest(String command) {}

    public record CalculatorRequest(String question) {}

    public record MultiToolsRequest(String question) {}
}
