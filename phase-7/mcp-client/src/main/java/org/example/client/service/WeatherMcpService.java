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

/**
 * MCP 客户端服务 - 天气服务调用者
 * 连接 MCP 服务端，调用天气工具（异步版本）
 */
@Service
public class WeatherMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public WeatherMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> {
                    return client.getClientInfo().name().equals("weather-server - weather-server");
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 weather-server 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        if (mcpAsyncClient == null) {
            System.err.println("MCP 客户端未初始化，请检查配置");
            return;
        }
        mcpAsyncClient.listTools()
                .doOnNext(tools -> {
                    System.out.println("MCP 客户端已连接，可用工具：" + tools.tools().stream()
                            .map(McpSchema.Tool::name)
                            .toList());
                })
                .doOnError(e -> {
                    System.err.println("连接 MCP 服务器失败: " + e.getMessage());
                    System.err.println("请确保 MCP 服务器已启动 (http://localhost:8080)");
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

    /**
     * 使用 AI 智能查询天气（流式输出）
     */
    public Flux<String> askWeatherAIStream(String question) {
        String city = extractCityFromQuestion(question);

        return Mono.zip(
                getCurrentWeather(city),
                getAirQuality(city),
                getLifeIndex(city)
        ).flatMapMany(tuple -> {
            String currentWeather = tuple.getT1();
            String airQuality = tuple.getT2();
            String lifeIndex = tuple.getT3();

            String prompt = String.format(
                    "基于以下%s的天气数据，回答用户的问题。\n\n天气数据：\n%s\n%s\n%s\n\n用户问题：%s",
                    city, currentWeather, airQuality, lifeIndex, question
            );

            return chatClientBuilder.build()
                    .prompt(prompt)
                    .stream()
                    .content();
        }).onErrorResume(e -> Flux.just("获取天气信息失败: " + e.getMessage()));
    }

    private String extractCityFromQuestion(String question) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "武汉", "西安", "重庆"};
        for (String city : cities) {
            if (question.contains(city)) {
                return city;
            }
        }
        return "北京";
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
