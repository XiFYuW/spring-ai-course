package org.example.server.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * MCP 服务器 - 天气工具提供者
 * 使用 Spring AI MCP 注解暴露天气查询功能（异步版本）
 */
@Component
public class WeatherTools {

    private final Random random = new Random();

    @McpTool(
            name = "getCurrentWeather",
            description = "获取指定城市的当前天气信息，包括天气状况、温度、湿度和风力"
    )
    public Mono<String> getCurrentWeather(
            @McpToolParam(description = "城市名称，例如：北京、上海、广州", required = true) String city) {
        System.out.println("city: " + city);
        String[] weathers = {"晴天", "多云", "阴天", "小雨", "中雨", "雷阵雨", "大雪"};
        String weather = weathers[random.nextInt(weathers.length)];
        int temperature = 15 + random.nextInt(20);
        int humidity = 40 + random.nextInt(50);
        int windLevel = 1 + random.nextInt(5);

        return Mono.just(String.format(
                "【%s】当前天气：%s，温度：%d°C，湿度：%d%%，风力：%d级，更新时间：%s",
                city, weather, temperature, humidity, windLevel,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));
    }

    @McpTool(
            name = "getWeatherForecast",
            description = "获取指定城市的未来几天天气预报，支持1-7天的预报"
    )
    public Mono<String> getWeatherForecast(
            @McpToolParam(description = "城市名称，例如：北京、上海、广州", required = true) String city,
            @McpToolParam(description = "预报天数，范围1-7天", required = true) Integer days) {

        StringBuilder forecast = new StringBuilder();
        forecast.append(String.format("【%s】未来%d天天气预报：\n", city, days));

        String[] weathers = {"晴天", "多云", "阴天", "小雨", "中雨", "雷阵雨"};

        for (int i = 0; i < Math.min(days, 7); i++) {
            String date = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
            String weather = weathers[random.nextInt(weathers.length)];
            int highTemp = 20 + random.nextInt(15);
            int lowTemp = highTemp - 5 - random.nextInt(5);

            forecast.append(String.format("  %s：%s，%d°C ~ %d°C\n",
                    date, weather, lowTemp, highTemp));
        }

        return Mono.just(forecast.toString());
    }

    @McpTool(
            name = "getAirQuality",
            description = "获取指定城市的空气质量指数(AQI)和健康建议"
    )
    public Mono<String> getAirQuality(
            @McpToolParam(description = "城市名称，例如：北京、上海、广州", required = true) String city) {

        int aqi = 30 + random.nextInt(150);
        String level;
        String advice;

        if (aqi <= 50) {
            level = "优";
            advice = "空气质量很好，可以放心户外活动";
        } else if (aqi <= 100) {
            level = "良";
            advice = "空气质量不错，适合户外活动";
        } else if (aqi <= 150) {
            level = "轻度污染";
            advice = "敏感人群应减少户外活动";
        } else {
            level = "中度污染";
            advice = "建议减少户外活动，佩戴口罩";
        }

        return Mono.just(String.format(
                "【%s】空气质量指数(AQI)：%d，等级：%s，建议：%s",
                city, aqi, level, advice
        ));
    }

    @McpTool(
            name = "getLifeIndex",
            description = "获取指定城市的生活指数建议，包括运动、洗车、穿衣等指数"
    )
    public Mono<String> getLifeIndex(
            @McpToolParam(description = "城市名称，例如：北京、上海、广州", required = true) String city) {

        String[] indices = {"运动", "洗车", "穿衣", "紫外线", "旅游", "感冒"};
        String[] levels = {"适宜", "较适宜", "一般", "较不适宜", "不适宜"};

        StringBuilder result = new StringBuilder();
        result.append(String.format("【%s】生活指数建议：\n", city));

        for (String index : indices) {
            String level = levels[random.nextInt(levels.length)];
            result.append(String.format("  %s指数：%s\n", index, level));
        }

        return Mono.just(result.toString());
    }
}
