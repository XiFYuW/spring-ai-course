package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 天气查询工具类
 * 演示模拟外部 API 调用的信息检索工具
 * 在实际应用中，这里应该调用真实的天气 API
 */
@Component
public class WeatherTools {

    private final Random random = new Random();
    
    // 模拟天气数据缓存
    private final Map<String, WeatherData> weatherCache = new HashMap<>();
    
    /**
     * 天气数据内部类
     */
    private static class WeatherData {
        String city;
        String condition;
        double temperature;
        int humidity;
        double windSpeed;
        String updateTime;
        
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

    /**
     * 获取指定城市的当前天气
     * 这是一个信息检索工具，模拟从外部天气 API 获取数据
     */
    @Tool(description = "获取指定城市的当前天气信息，包括温度、天气状况、湿度、风速等")
    public String getCurrentWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        
        // 模拟 API 调用延迟
        simulateApiDelay();
        
        // 生成模拟天气数据（实际应用中应调用真实天气 API）
        WeatherData data = generateMockWeather(city);
        weatherCache.put(city, data);
        
        String result = formatWeatherData(data);
        System.out.println("[工具调用] getCurrentWeather(" + city + ") -> 数据已获取");
        return result;
    }

    /**
     * 获取指定城市的天气预报
     */
    @Tool(description = "获取指定城市未来几天的天气预报")
    public String getWeatherForecast(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city,
            @ToolParam(description = "预报天数（1-7天）", required = false) Integer days) {
        
        int forecastDays = days != null ? Math.min(Math.max(days, 1), 7) : 3;
        simulateApiDelay();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🌤️ %s 未来 %d 天天气预报\n", city, forecastDays));
        sb.append("═══════════════════════════════════════\n\n");
        
        LocalDateTime baseTime = LocalDateTime.now();
        String[] conditions = {"晴", "多云", "阴", "小雨", "中雨", "雷阵雨"};
        
        for (int i = 1; i <= forecastDays; i++) {
            LocalDateTime forecastTime = baseTime.plusDays(i);
            String date = forecastTime.format(DateTimeFormatter.ofPattern("MM月dd日"));
            String dayOfWeek = getDayOfWeekChinese(forecastTime.getDayOfWeek().getValue());
            
            String condition = conditions[random.nextInt(conditions.length)];
            double highTemp = 20 + random.nextInt(15);
            double lowTemp = highTemp - 5 - random.nextInt(5);
            int humidity = 40 + random.nextInt(40);
            
            sb.append(String.format(
                "📅 %s (%s)\n" +
                "   天气: %s\n" +
                "   温度: %.0f°C ~ %.0f°C\n" +
                "   湿度: %d%%\n\n",
                date, dayOfWeek, condition, lowTemp, highTemp, humidity
            ));
        }
        
        System.out.println("[工具调用] getWeatherForecast(" + city + ", " + forecastDays + "天) -> 预报已生成");
        return sb.toString();
    }

    /**
     * 获取空气质量指数
     */
    @Tool(description = "获取指定城市的空气质量指数(AQI)和空气质量等级")
    public String getAirQuality(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        
        simulateApiDelay();
        
        int aqi = 30 + random.nextInt(150);
        String level;
        String emoji;
        String advice;
        
        if (aqi <= 50) {
            level = "优";
            emoji = "🟢";
            advice = "空气质量很好，可以放心进行户外活动";
        } else if (aqi <= 100) {
            level = "良";
            emoji = "🟡";
            advice = "空气质量一般，敏感人群应减少户外活动";
        } else if (aqi <= 150) {
            level = "轻度污染";
            emoji = "🟠";
            advice = "儿童、老年人及心脏病、呼吸系统疾病患者应减少长时间、高强度的户外锻炼";
        } else {
            level = "中度污染";
            emoji = "🔴";
            advice = "一般人群适量减少户外运动，敏感人群应避免户外活动";
        }
        
        String result = String.format(
            "🏭 %s 空气质量报告\n" +
            "═══════════════════════════════════════\n" +
            "   AQI 指数: %d\n" +
            "   空气质量: %s %s\n" +
            "   建议: %s\n",
            city, aqi, emoji, level, advice
        );
        
        System.out.println("[工具调用] getAirQuality(" + city + ") -> AQI: " + aqi);
        return result;
    }

    /**
     * 比较两个城市的天气
     */
    @Tool(description = "比较两个城市的天气情况")
    public String compareWeather(
            @ToolParam(description = "第一个城市") String city1,
            @ToolParam(description = "第二个城市") String city2) {
        
        simulateApiDelay();
        
        WeatherData data1 = generateMockWeather(city1);
        WeatherData data2 = generateMockWeather(city2);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🌡️ 天气对比: %s vs %s\n", city1, city2));
        sb.append("═══════════════════════════════════════\n\n");
        
        sb.append(String.format(
            "📍 %s:\n" +
            "   天气: %s\n" +
            "   温度: %.1f°C\n" +
            "   湿度: %d%%\n" +
            "   风速: %.1f m/s\n\n",
            city1, data1.condition, data1.temperature, data1.humidity, data1.windSpeed
        ));
        
        sb.append(String.format(
            "📍 %s:\n" +
            "   天气: %s\n" +
            "   温度: %.1f°C\n" +
            "   湿度: %d%%\n" +
            "   风速: %.1f m/s\n\n",
            city2, data2.condition, data2.temperature, data2.humidity, data2.windSpeed
        ));
        
        // 温差分析
        double tempDiff = Math.abs(data1.temperature - data2.temperature);
        String warmerCity = data1.temperature > data2.temperature ? city1 : city2;
        sb.append(String.format("📊 对比分析:\n   %s 比 %s 高 %.1f°C", 
            warmerCity, warmerCity.equals(city1) ? city2 : city1, tempDiff));
        
        System.out.println("[工具调用] compareWeather(" + city1 + ", " + city2 + ") -> 对比完成");
        return sb.toString();
    }

    /**
     * 生成模拟天气数据
     */
    private WeatherData generateMockWeather(String city) {
        String[] conditions = {"晴", "多云", "阴", "小雨", "中雨"};
        String condition = conditions[random.nextInt(conditions.length)];
        
        // 根据城市名称生成相对稳定的随机数（模拟真实 API 的行为）
        int cityHash = Math.abs(city.hashCode());
        double baseTemp = 15 + (cityHash % 15);
        double temperature = baseTemp + random.nextInt(5);
        int humidity = 40 + (cityHash % 40);
        double windSpeed = 1 + random.nextInt(10);
        
        String updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        
        return new WeatherData(city, condition, temperature, humidity, windSpeed, updateTime);
    }

    /**
     * 格式化天气数据为字符串
     */
    private String formatWeatherData(WeatherData data) {
        String emoji = getWeatherEmoji(data.condition);
        
        return String.format(
            "🌍 %s 当前天气 %s\n" +
            "═══════════════════════════════════════\n" +
            "   天气状况: %s\n" +
            "   温度: %.1f°C\n" +
            "   湿度: %d%%\n" +
            "   风速: %.1f m/s\n" +
            "   更新时间: %s\n",
            data.city, emoji, data.condition, data.temperature, 
            data.humidity, data.windSpeed, data.updateTime
        );
    }

    /**
     * 获取天气表情
     */
    private String getWeatherEmoji(String condition) {
        return switch (condition) {
            case "晴" -> "☀️";
            case "多云" -> "⛅";
            case "阴" -> "☁️";
            case "小雨" -> "🌦️";
            case "中雨" -> "🌧️";
            case "雷阵雨" -> "⛈️";
            default -> "🌤️";
        };
    }

    /**
     * 获取星期几的中文名称
     */
    private String getDayOfWeekChinese(int dayOfWeek) {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[dayOfWeek - 1];
    }

    /**
     * 模拟 API 调用延迟
     */
    private void simulateApiDelay() {
        try {
            Thread.sleep(100 + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
